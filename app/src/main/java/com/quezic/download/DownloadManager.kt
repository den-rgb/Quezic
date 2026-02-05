package com.quezic.download

import android.content.Context
import androidx.work.*
import com.quezic.domain.model.DownloadItem
import com.quezic.domain.model.DownloadState
import com.quezic.domain.model.Song
import com.quezic.domain.model.StreamQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    
    private val _downloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadItem>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val activeDownloads: StateFlow<List<DownloadItem>> = _activeDownloads.asStateFlow()

    fun downloadSong(song: Song, quality: StreamQuality = StreamQuality.HIGH): UUID {
        val downloadItem = DownloadItem(
            song = song,
            state = DownloadState.Queued,
            quality = quality
        )
        
        _downloads.update { it + (song.id to downloadItem) }
        updateActiveDownloads()
        
        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_SONG_ID, song.id)
            .putString(DownloadWorker.KEY_SONG_TITLE, song.title)
            .putString(DownloadWorker.KEY_SONG_ARTIST, song.artist)
            .putString(DownloadWorker.KEY_SOURCE_TYPE, song.sourceType.name)
            .putString(DownloadWorker.KEY_SOURCE_ID, song.sourceId)
            .putString(DownloadWorker.KEY_QUALITY, quality.name)
            .putString(DownloadWorker.KEY_THUMBNAIL_URL, song.thumbnailUrl)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(DOWNLOAD_TAG)
            .addTag(song.id)
            .build()

        workManager.enqueueUniqueWork(
            "download_${song.id}",
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )

        // Observe work status
        workManager.getWorkInfoByIdLiveData(downloadRequest.id).observeForever { workInfo ->
            workInfo?.let { info ->
                updateDownloadState(song.id, info)
            }
        }

        return downloadRequest.id
    }

    fun cancelDownload(songId: String) {
        workManager.cancelUniqueWork("download_$songId")
        _downloads.update { downloads ->
            downloads[songId]?.let { item ->
                downloads + (songId to item.copy(state = DownloadState.Idle))
            } ?: downloads
        }
        updateActiveDownloads()
    }

    fun cancelAllDownloads() {
        workManager.cancelAllWorkByTag(DOWNLOAD_TAG)
        _downloads.update { downloads ->
            downloads.mapValues { (_, item) ->
                item.copy(state = DownloadState.Idle)
            }
        }
        updateActiveDownloads()
    }

    fun retryDownload(songId: String) {
        _downloads.value[songId]?.let { item ->
            downloadSong(item.song, item.quality)
        }
    }

    fun removeFromQueue(songId: String) {
        cancelDownload(songId)
        _downloads.update { it - songId }
        updateActiveDownloads()
    }

    private fun updateDownloadState(songId: String, workInfo: WorkInfo) {
        val newState = when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> DownloadState.Queued
            WorkInfo.State.RUNNING -> {
                val progress = workInfo.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f)
                DownloadState.Downloading(progress)
            }
            WorkInfo.State.SUCCEEDED -> DownloadState.Completed
            WorkInfo.State.FAILED -> {
                val error = workInfo.outputData.getString(DownloadWorker.KEY_ERROR) ?: "Download failed"
                DownloadState.Failed(error)
            }
            WorkInfo.State.CANCELLED -> DownloadState.Idle
            WorkInfo.State.BLOCKED -> DownloadState.Queued
        }

        _downloads.update { downloads ->
            downloads[songId]?.let { item ->
                downloads + (songId to item.copy(state = newState))
            } ?: downloads
        }
        updateActiveDownloads()
    }

    private fun updateActiveDownloads() {
        _activeDownloads.value = _downloads.value.values
            .filter { it.state is DownloadState.Queued || it.state is DownloadState.Downloading }
            .toList()
    }

    fun getDownloadState(songId: String): DownloadState {
        return _downloads.value[songId]?.state ?: DownloadState.Idle
    }

    fun isDownloading(songId: String): Boolean {
        val state = _downloads.value[songId]?.state
        return state is DownloadState.Queued || state is DownloadState.Downloading
    }

    companion object {
        const val DOWNLOAD_TAG = "quezic_download"
    }
}
