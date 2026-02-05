package com.quezic.domain.model

sealed class DownloadState {
    object Idle : DownloadState()
    object Queued : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

data class DownloadItem(
    val song: Song,
    val state: DownloadState = DownloadState.Idle,
    val quality: StreamQuality = StreamQuality.HIGH
)

enum class StreamQuality {
    LOW,    // ~64kbps
    MEDIUM, // ~128kbps
    HIGH,   // ~256kbps
    BEST    // Best available
}
