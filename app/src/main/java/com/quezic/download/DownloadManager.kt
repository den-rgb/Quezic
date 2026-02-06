package com.quezic.download

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
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
import java.util.LinkedList
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages song downloads with a sharded concurrent queue.
 *
 * Up to [MAX_CONCURRENT_DOWNLOADS] WorkManager jobs run simultaneously.
 * When one finishes (success or failure), the next item in the pending queue
 * is started to fill the open slot. This balances:
 * - Faster total download throughput (multiple shards)
 * - Not overwhelming the proxy server (capped concurrency)
 * - Independent failure handling (one failure doesn't affect others)
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DownloadManager"
        const val DOWNLOAD_TAG = "quezic_download"
        // Match the proxy server's MAX_CONCURRENT_EXTRACTIONS
        const val MAX_CONCURRENT_DOWNLOADS = 2
    }

    private val workManager = WorkManager.getInstance(context)

    private val _downloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadItem>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val activeDownloads: StateFlow<List<DownloadItem>> = _activeDownloads.asStateFlow()

    // Self-managed queue: pending downloads waiting to start
    private val pendingQueue = LinkedList<QueueEntry>()

    // Currently running downloads (up to MAX_CONCURRENT_DOWNLOADS)
    private val activeEntries = mutableListOf<QueueEntry>()
    private val activeObservers = mutableMapOf<UUID, Observer<WorkInfo>>()

    // Total count for "X of Y" display
    private var totalQueuedThisBatch = 0
    private var completedThisBatch = 0

    private data class QueueEntry(
        val song: Song,
        val quality: StreamQuality,
        var workRequestId: UUID? = null
    )

    fun downloadSong(song: Song, quality: StreamQuality = StreamQuality.HIGH): UUID {
        // Don't enqueue if already downloading or queued
        if (isDownloading(song.id)) {
            Log.d(TAG, "Song ${song.id} already in download queue, skipping")
            return activeEntries.find { it.song.id == song.id }?.workRequestId ?: UUID.randomUUID()
        }

        val downloadItem = DownloadItem(
            song = song,
            state = DownloadState.Queued,
            quality = quality
        )

        _downloads.update { it + (song.id to downloadItem) }
        updateActiveDownloads()

        val entry = QueueEntry(song, quality)
        pendingQueue.add(entry)
        totalQueuedThisBatch = pendingQueue.size + activeEntries.size + completedThisBatch

        Log.d(TAG, "Queued download for '${song.title}' (total active: ${activeEntries.size}, pending: ${pendingQueue.size})")

        // If we have open download slots, start filling them
        if (activeEntries.isEmpty()) {
            completedThisBatch = 0
            totalQueuedThisBatch = pendingQueue.size
        }
        fillDownloadSlots()

        return entry.workRequestId ?: UUID.randomUUID()
    }

    /**
     * Fills available download slots from the pending queue.
     * Called when new items are enqueued or when a download finishes.
     */
    private fun fillDownloadSlots() {
        while (activeEntries.size < MAX_CONCURRENT_DOWNLOADS && pendingQueue.isNotEmpty()) {
            startNextDownload()
        }

        // If nothing is active and queue is empty, reset
        if (activeEntries.isEmpty() && pendingQueue.isEmpty()) {
            totalQueuedThisBatch = 0
            completedThisBatch = 0
            Log.d(TAG, "Download queue empty, all done")
        }
    }

    private fun startNextDownload() {
        val entry = pendingQueue.poll() ?: return

        val song = entry.song
        Log.d(TAG, "Starting download for '${song.title}' (active: ${activeEntries.size + 1}, pending: ${pendingQueue.size})")

        // Update state to Downloading
        _downloads.update { downloads ->
            downloads[song.id]?.let { item ->
                downloads + (song.id to item.copy(state = DownloadState.Downloading(0f)))
            } ?: downloads
        }
        updateActiveDownloads()

        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_SONG_ID, song.id)
            .putString(DownloadWorker.KEY_SONG_TITLE, song.title)
            .putString(DownloadWorker.KEY_SONG_ARTIST, song.artist)
            .putString(DownloadWorker.KEY_SOURCE_TYPE, song.sourceType.name)
            .putString(DownloadWorker.KEY_SOURCE_ID, song.sourceId)
            .putString(DownloadWorker.KEY_QUALITY, entry.quality.name)
            .putString(DownloadWorker.KEY_THUMBNAIL_URL, song.thumbnailUrl)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(DOWNLOAD_TAG)
            .addTag("song_${song.id}")
            .build()

        entry.workRequestId = downloadRequest.id
        activeEntries.add(entry)

        // Enqueue as independent work (not chained)
        workManager.enqueueUniqueWork(
            "download_${song.id}",
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )

        // Observe work status and trigger slot refill on completion
        val observer = Observer<WorkInfo> { workInfo ->
            workInfo ?: return@Observer
            updateDownloadState(song.id, workInfo)

            // When this download reaches a terminal state, free the slot and fill it
            if (workInfo.state.isFinished) {
                Log.d(TAG, "Download '${song.title}' finished with state: ${workInfo.state}")
                completedThisBatch++

                // Clean up observer for this specific download
                activeObservers.remove(downloadRequest.id)?.let {
                    workManager.getWorkInfoByIdLiveData(downloadRequest.id).removeObserver(it)
                }

                // Remove from active entries
                activeEntries.removeAll { it.workRequestId == downloadRequest.id }

                // Fill the now-open slot with the next pending download
                fillDownloadSlots()
            }
        }
        activeObservers[downloadRequest.id] = observer
        workManager.getWorkInfoByIdLiveData(downloadRequest.id).observeForever(observer)
    }

    fun cancelDownload(songId: String) {
        // Remove from pending queue if not yet started
        pendingQueue.removeAll { it.song.id == songId }

        // Cancel active WorkManager job if it's one of the active entries
        val activeEntry = activeEntries.find { it.song.id == songId }
        if (activeEntry != null) {
            activeEntry.workRequestId?.let { id ->
                workManager.cancelWorkById(id)
                activeObservers.remove(id)?.let {
                    workManager.getWorkInfoByIdLiveData(id).removeObserver(it)
                }
            }
            activeEntries.remove(activeEntry)
        }

        // Also cancel by tag as fallback
        workManager.cancelAllWorkByTag("song_$songId")

        _downloads.update { downloads ->
            downloads[songId]?.let { item ->
                downloads + (songId to item.copy(state = DownloadState.Idle))
            } ?: downloads
        }
        updateActiveDownloads()

        // Fill the freed slot
        fillDownloadSlots()
    }

    fun cancelAllDownloads() {
        // Clear the pending queue
        pendingQueue.clear()

        // Cancel all active downloads
        activeEntries.forEach { entry ->
            entry.workRequestId?.let { workManager.cancelWorkById(it) }
        }
        activeEntries.clear()

        // Clean up all observers
        activeObservers.forEach { (id, observer) ->
            workManager.getWorkInfoByIdLiveData(id).removeObserver(observer)
        }
        activeObservers.clear()

        // Cancel all by tag as fallback
        workManager.cancelAllWorkByTag(DOWNLOAD_TAG)

        totalQueuedThisBatch = 0
        completedThisBatch = 0

        _downloads.update { downloads ->
            downloads.mapValues { (_, item) ->
                if (item.state is DownloadState.Queued || item.state is DownloadState.Downloading) {
                    item.copy(state = DownloadState.Idle)
                } else item
            }
        }
        updateActiveDownloads()
    }

    fun retryDownload(songId: String) {
        _downloads.value[songId]?.let { item ->
            _downloads.update { it - songId }
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

    /**
     * Returns the current queue position for a song (1-based), or null if not queued.
     * Active entries are numbered first, then pending queue items.
     */
    fun getQueuePosition(songId: String): Int? {
        // Check active entries first
        val activeIndex = activeEntries.indexOfFirst { it.song.id == songId }
        if (activeIndex >= 0) return completedThisBatch + activeIndex + 1

        // Check pending queue
        val queueIndex = pendingQueue.indexOfFirst { it.song.id == songId }
        if (queueIndex >= 0) return completedThisBatch + activeEntries.size + queueIndex + 1

        return null
    }

    /**
     * Returns the total number of songs in the current download batch.
     */
    fun getQueueSize(): Int = totalQueuedThisBatch
}
