package com.quezic.download

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quezic.MainActivity
import com.quezic.QuezicApp
import com.quezic.data.local.dao.SongDao
import com.quezic.data.remote.MusicExtractorService
import com.quezic.domain.model.SourceType
import com.quezic.domain.model.StreamQuality
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val extractorService: MusicExtractorService,
    private val songDao: SongDao
) : CoroutineWorker(context, params) {

    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val songTitle = inputData.getString(KEY_SONG_TITLE) ?: "Unknown"
        val songArtist = inputData.getString(KEY_SONG_ARTIST) ?: "Unknown"
        val sourceTypeName = inputData.getString(KEY_SOURCE_TYPE) ?: return@withContext Result.failure()
        val sourceId = inputData.getString(KEY_SOURCE_ID) ?: return@withContext Result.failure()
        val qualityName = inputData.getString(KEY_QUALITY) ?: StreamQuality.HIGH.name

        val sourceType = SourceType.valueOf(sourceTypeName)
        val quality = StreamQuality.valueOf(qualityName)

        try {
            // Show foreground notification
            setForeground(createForegroundInfo(songTitle, songArtist, 0))

            // Get download URL (prefers progressive/direct streams over HLS)
            val streamUrl = extractorService.getDownloadUrl(sourceType, sourceId, quality)
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "Could not get download URL (no direct stream available)")
                )

            // Check if we got an HLS URL (which can't be directly downloaded)
            if (streamUrl.contains(".m3u8") || streamUrl.contains("/playlist/")) {
                Log.e(TAG, "Got HLS URL instead of direct stream - download not supported: $streamUrl")
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "This song only has streaming format (HLS) - direct download not available")
                )
            }

            // Create downloads directory
            val downloadsDir = File(context.getExternalFilesDir(null), "music")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Determine file extension based on content type or URL
            // YouTube audio via proxy is typically M4A/AAC, SoundCloud can be MP3
            // Note: Proxy URLs don't have file extensions, so we rely on source type
            val extension = when {
                streamUrl.contains(".mp3") -> "mp3"
                streamUrl.contains(".m4a") -> "m4a"
                streamUrl.contains(".webm") -> "webm"
                streamUrl.contains(".opus") -> "opus"
                sourceType == SourceType.SOUNDCLOUD -> "mp3"
                sourceType == SourceType.YOUTUBE -> "m4a" // YouTube audio is M4A/AAC
                else -> "m4a"
            }

            // Create file name
            val sanitizedTitle = "$songArtist - $songTitle"
                .replace(Regex("[^a-zA-Z0-9\\s\\-.]"), "")
                .take(200)
            val file = File(downloadsDir, "$sanitizedTitle.$extension")

            Log.d(TAG, "Downloading from: $streamUrl")
            Log.d(TAG, "Saving to: ${file.absolutePath}")

            // Actually download the audio file
            // Note: followRedirects handles both same-protocol and cross-protocol redirects
            // Increased timeouts for large files and slow connections
            val client = OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)  // 10 minutes for slow downloads
                .writeTimeout(600, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.SECONDS)    // No overall timeout - let it complete
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()

            val requestBuilder = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")

            // Add YouTube-specific headers only for direct YouTube URLs (not proxy URLs)
            // Proxy URLs handle authentication internally
            if (sourceType == SourceType.YOUTUBE && streamUrl.contains("googlevideo.com")) {
                requestBuilder
                    .header("Origin", "https://www.youtube.com")
                    .header("Referer", "https://www.youtube.com/")
            }

            val request = requestBuilder.build()
            var response = client.newCall(request).execute()

            // Handle redirects manually if OkHttp didn't follow them
            var redirectCount = 0
            while (response.code in 300..399 && redirectCount < 5) {
                val location = response.header("Location")
                if (location == null) {
                    Log.e(TAG, "Redirect without Location header")
                    break
                }
                Log.d(TAG, "Following redirect to: ${location.take(100)}...")
                response.close()
                
                val redirectRequest = Request.Builder()
                    .url(location)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .build()
                response = client.newCall(redirectRequest).execute()
                redirectCount++
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed with code: ${response.code}")
                response.close()
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "Download failed: HTTP ${response.code}")
                )
            }

            val body = response.body ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Empty response body")
            )

            val contentLength = body.contentLength()
            Log.d(TAG, "Content length: $contentLength bytes")

            // Download with progress updates
            FileOutputStream(file).use { outputStream ->
                body.byteStream().use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead: Long = 0
                    var lastProgress = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress
                        if (contentLength > 0) {
                            val progress = ((totalBytesRead * 100) / contentLength).toInt()
                            if (progress != lastProgress && progress % 5 == 0) {
                                lastProgress = progress
                                setProgress(workDataOf(KEY_PROGRESS to progress / 100f))
                                setForeground(createForegroundInfo(songTitle, songArtist, progress))
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Download complete: ${file.absolutePath} (${file.length()} bytes)")

            // Update database with local path
            songDao.updateLocalPath(songId, file.absolutePath)

            // Show completion notification
            showCompletionNotification(songTitle, songArtist)

            Result.success(workDataOf(KEY_FILE_PATH to file.absolutePath))
        } catch (e: Exception) {
            showErrorNotification(songTitle, e.message ?: "Download failed")
            Result.failure(workDataOf(KEY_ERROR to e.message))
        }
    }

    private fun createForegroundInfo(title: String, artist: String, progress: Int): ForegroundInfo {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, QuezicApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText("$artist - $title")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        // Android 14 (API 34) requires specifying foreground service type
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(title: String, artist: String) {
        val notification = NotificationCompat.Builder(context, QuezicApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText("$artist - $title")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(title: String, error: String) {
        val notification = NotificationCompat.Builder(context, QuezicApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Download failed")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "DownloadWorker"
        
        const val KEY_SONG_ID = "song_id"
        const val KEY_SONG_TITLE = "song_title"
        const val KEY_SONG_ARTIST = "song_artist"
        const val KEY_SOURCE_TYPE = "source_type"
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_QUALITY = "quality"
        const val KEY_THUMBNAIL_URL = "thumbnail_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR = "error"

        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        private const val ERROR_NOTIFICATION_ID = 1003
    }
}
