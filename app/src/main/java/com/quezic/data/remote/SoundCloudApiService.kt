package com.quezic.data.remote

import android.util.Log
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a track from a SoundCloud playlist.
 */
data class SoundCloudTrack(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val url: String
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

/**
 * Represents a SoundCloud playlist.
 */
data class SoundCloudPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val uploaderName: String?,
    val tracks: List<SoundCloudTrack>
) {
    val trackCount: Int get() = tracks.size
}

/**
 * Service to fetch SoundCloud playlist data using the NewPipe extractor.
 */
@Singleton
class SoundCloudApiService @Inject constructor() {

    companion object {
        private const val TAG = "SoundCloudApi"

        // Playlist/set URL: soundcloud.com/<user>/sets/<playlist>
        // Path segments use [^/\\s?#]+ to match any valid URL chars (dots, percent-encoding, etc.)
        private val SOUNDCLOUD_PLAYLIST_REGEX = Regex(
            "(?:https?://)?(?:m\\.|www\\.)?soundcloud\\.com/[^/\\s?#]+/sets/[^/\\s?#]+.*"
        )
        // Likes URL: soundcloud.com/<user>/likes
        private val SOUNDCLOUD_LIKES_REGEX = Regex(
            "(?:https?://)?(?:m\\.|www\\.)?soundcloud\\.com/[^/\\s?#]+/likes.*"
        )
        // Short share URL from SoundCloud mobile app: on.soundcloud.com/<shortcode>
        // These redirect to the full URL – we accept them and let fetchPlaylist resolve them.
        private val SOUNDCLOUD_SHORT_URL_REGEX = Regex(
            "(?:https?://)?on\\.soundcloud\\.com/[^\\s]+"
        )
    }

    /**
     * Validates that a URL is a valid SoundCloud playlist, likes, or share URL.
     * Accepts:
     * - Full playlist URLs: https://soundcloud.com/user/sets/playlist-name?si=...
     * - Likes URLs: https://soundcloud.com/user/likes
     * - Short share URLs: https://on.soundcloud.com/AbCdEf (from SoundCloud app share)
     * - Mobile URLs: https://m.soundcloud.com/...
     */
    fun isValidSoundCloudPlaylistUrl(url: String): Boolean {
        val trimmed = url.trim()
        val isValid = SOUNDCLOUD_PLAYLIST_REGEX.matches(trimmed) ||
                SOUNDCLOUD_LIKES_REGEX.matches(trimmed) ||
                SOUNDCLOUD_SHORT_URL_REGEX.matches(trimmed)
        Log.d(TAG, "isValidSoundCloudPlaylistUrl('${trimmed.take(80)}'): $isValid")
        return isValid
    }

    // OkHttp client for resolving short URLs (disabled redirect following to read Location header)
    private val redirectClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    /**
     * Resolves a SoundCloud short URL (on.soundcloud.com) to its full URL.
     * Returns the original URL if it's not a short URL or resolution fails.
     */
    private fun resolveShortUrl(url: String): String {
        if (!SOUNDCLOUD_SHORT_URL_REGEX.matches(url.trim())) return url

        return try {
            val request = Request.Builder().url(url.trim()).head().build()
            val response = redirectClient.newCall(request).execute()
            val location = response.header("Location")
            response.close()
            if (location != null) {
                Log.d(TAG, "Resolved short URL -> $location")
                location
            } else {
                Log.w(TAG, "Short URL did not redirect, using as-is")
                url
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve short URL: ${e.message}")
            url
        }
    }

    /**
     * Fetches a SoundCloud playlist and its tracks.
     */
    suspend fun fetchPlaylist(url: String): Result<SoundCloudPlaylist> = withContext(Dispatchers.IO) {
        try {
            // Resolve short URLs (on.soundcloud.com/xxx) to full URLs before extraction
            val resolvedUrl = resolveShortUrl(url)

            val service = ServiceList.SoundCloud
            val extractor = service.getPlaylistExtractor(resolvedUrl.trim())
            extractor.fetchPage()

            val tracks = mutableListOf<SoundCloudTrack>()
            
            // Get initial page
            var page = extractor.initialPage
            for (item in page.items) {
                if (item is StreamInfoItem && item.duration > 0) {
                    tracks.add(
                        SoundCloudTrack(
                            title = item.name,
                            artist = item.uploaderName ?: "Unknown Artist",
                            durationMs = item.duration * 1000,
                            thumbnailUrl = item.thumbnails.maxByOrNull { it.width }?.url
                                ?: item.thumbnails.firstOrNull()?.url,
                            url = item.url
                        )
                    )
                }
            }

            // Get subsequent pages
            while (page.hasNextPage()) {
                try {
                    page = extractor.getPage(page.nextPage)
                    for (item in page.items) {
                        if (item is StreamInfoItem && item.duration > 0) {
                            tracks.add(
                                SoundCloudTrack(
                                    title = item.name,
                                    artist = item.uploaderName ?: "Unknown Artist",
                                    durationMs = item.duration * 1000,
                                    thumbnailUrl = item.thumbnails.maxByOrNull { it.width }?.url
                                        ?: item.thumbnails.firstOrNull()?.url,
                                    url = item.url
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching next page: ${e.message}")
                    break
                }
            }

            val playlist = SoundCloudPlaylist(
                id = extractor.id ?: url.hashCode().toString(),
                name = extractor.name ?: "SoundCloud Playlist",
                description = extractor.description?.content,
                coverUrl = extractor.thumbnails.maxByOrNull { it.width }?.url
                    ?: extractor.thumbnails.firstOrNull()?.url,
                uploaderName = extractor.uploaderName,
                tracks = tracks
            )

            Log.d(TAG, "Fetched playlist '${playlist.name}' with ${playlist.trackCount} tracks")
            Result.success(playlist)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch SoundCloud playlist: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Converts a SoundCloudTrack to a SearchResult for use in the app.
     */
    fun trackToSearchResult(track: SoundCloudTrack): SearchResult {
        return SearchResult(
            id = "sc_${track.url.hashCode()}",
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl,
            duration = track.durationMs,
            sourceType = SourceType.SOUNDCLOUD,
            sourceId = track.url,
            sourceUrl = track.url
        )
    }
}
