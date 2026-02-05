package com.quezic.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.quezic.domain.model.PlayerState
import com.quezic.domain.model.RepeatMode
import com.quezic.domain.model.Song
import com.quezic.domain.model.SourceType
import com.quezic.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository
) {
    companion object {
        private const val TAG = "PlayerController"
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var positionUpdateJob: kotlinx.coroutines.Job? = null

    init {
        initializePlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        // Create smart DataSource factory that handles YouTube streams properly
        val dataSourceFactory = YouTubeDataSourceFactory.createSmart(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        
        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Log.d(TAG, "Playback ready, duration: ${exoPlayer?.duration}")
                                _playerState.update { 
                                    it.copy(
                                        duration = exoPlayer?.duration ?: 0L,
                                        error = null
                                    ) 
                                }
                            }
                            Player.STATE_ENDED -> {
                                handleSongEnd()
                            }
                            Player.STATE_BUFFERING -> {
                                Log.d(TAG, "Buffering...")
                            }
                            Player.STATE_IDLE -> {
                                Log.d(TAG, "Player idle")
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            startPositionUpdates()
                        } else {
                            stopPositionUpdates()
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        
                        // Check if this is a YouTube 403 error (check both message and cause)
                        val currentSong = _playerState.value.currentSong
                        val errorDetails = "${error.message} ${error.cause?.message ?: ""}"
                        val is403Error = errorDetails.contains("403") || 
                                         error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                        val isYouTube403 = is403Error && currentSong?.sourceType == SourceType.YOUTUBE
                        
                        Log.d(TAG, "Error details: $errorDetails, is403=$is403Error, isYouTube=$isYouTube403")
                        
                        val errorMessage = when {
                            isYouTube403 -> 
                                "YouTube requires authentication. Tap to open in YouTube app."
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                                "Network error. Check your connection."
                            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                                "Stream unavailable"
                            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                            error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                                "Invalid stream format"
                            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                                "Audio decoder not available"
                            else ->
                                "Playback error: ${error.message}"
                        }
                        
                        // Extract YouTube video ID for fallback
                        val videoId = if (isYouTube403 && currentSong != null) {
                            extractYouTubeVideoId(currentSong.sourceId)
                        } else null
                        
                        Log.d(TAG, "YouTube fallback available: ${videoId != null}, videoId: $videoId")
                        
                        _playerState.update { 
                            it.copy(
                                error = errorMessage, 
                                isPlaying = false,
                                canOpenInYouTube = isYouTube403 && videoId != null,
                                youtubeVideoId = videoId
                            ) 
                        }
                    }
                })
            }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                delay(500)
                exoPlayer?.let { player ->
                    _playerState.update { 
                        it.copy(
                            currentPosition = player.currentPosition,
                            duration = if (player.duration > 0) player.duration else it.duration
                        ) 
                    }
                }
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
    }

    private fun handleSongEnd() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (_playerState.value.hasNext) {
                    playNext()
                } else {
                    _playerState.update { it.copy(isPlaying = false, currentPosition = 0) }
                }
            }
        }
    }

    suspend fun playSong(song: Song): Boolean {
        // Update state to show loading
        _playerState.update { 
            it.copy(
                currentSong = song,
                isPlaying = false,
                error = null
            ) 
        }

        // For downloaded songs, use the local file path directly
        val playbackUri: Uri = if (song.isDownloaded && song.localPath != null) {
            Log.d(TAG, "Playing downloaded song from: ${song.localPath}")
            // Check if file exists
            val file = java.io.File(song.localPath)
            if (!file.exists()) {
                Log.e(TAG, "Downloaded file not found: ${song.localPath}")
                _playerState.update { 
                    it.copy(error = "Downloaded file not found. Try re-downloading.") 
                }
                return false
            }
            Uri.fromFile(file)
        } else {
            // Get the stream URL for non-downloaded songs
            val streamUrl = musicRepository.getStreamUrl(song)
            
            if (streamUrl == null) {
                // Handle error - no stream URL available
                val errorMessage = if (song.sourceType == com.quezic.domain.model.SourceType.YOUTUBE) {
                    "YouTube playback unavailable. Try SoundCloud instead."
                } else {
                    "Could not play: Unable to get audio stream"
                }
                _playerState.update { 
                    it.copy(error = errorMessage) 
                }
                return false
            }
            
            Log.d(TAG, "Playing stream from: ${streamUrl.take(100)}...")
            Uri.parse(streamUrl)
        }

        try {
            // Create media item
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(playbackUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.thumbnailUrl?.let { Uri.parse(it) })
                        .build()
                )
                .build()

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            _playerState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true,
                    currentPosition = 0,
                    duration = song.duration,
                    queue = if (it.queue.isEmpty() || !it.queue.contains(song)) listOf(song) else it.queue,
                    currentIndex = if (it.queue.contains(song)) it.queue.indexOf(song) else 0,
                    error = null
                )
            }
            return true
        } catch (e: Exception) {
            _playerState.update { 
                it.copy(
                    error = "Playback error: ${e.message}"
                ) 
            }
            return false
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        val queue = if (_playerState.value.shuffleEnabled) {
            songs.shuffled()
        } else {
            songs
        }
        
        _playerState.update {
            it.copy(
                queue = queue,
                currentIndex = startIndex
            )
        }

        scope.launch {
            playSong(queue[startIndex])
            _playerState.update {
                it.copy(
                    queue = queue,
                    currentIndex = startIndex
                )
            }
        }
    }

    fun addToQueue(song: Song) {
        _playerState.update {
            it.copy(queue = it.queue + song)
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun playNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return
        
        val nextIndex = when {
            state.shuffleEnabled -> state.queue.indices.random()
            state.currentIndex < state.queue.lastIndex -> state.currentIndex + 1
            state.repeatMode == RepeatMode.ALL -> 0
            else -> return
        }
        
        scope.launch {
            val nextSong = state.queue[nextIndex]
            playSong(nextSong)
            _playerState.update {
                it.copy(
                    queue = state.queue,
                    currentIndex = nextIndex
                )
            }
        }
    }

    fun playPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return
        
        // If more than 3 seconds in, restart current song
        if (state.currentPosition > 3000) {
            seekTo(0)
            return
        }
        
        val prevIndex = when {
            state.currentIndex > 0 -> state.currentIndex - 1
            state.repeatMode == RepeatMode.ALL -> state.queue.lastIndex
            else -> return
        }
        
        scope.launch {
            val prevSong = state.queue[prevIndex]
            playSong(prevSong)
            _playerState.update {
                it.copy(
                    queue = state.queue,
                    currentIndex = prevIndex
                )
            }
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        _playerState.update { 
            it.copy(currentPosition = position.coerceIn(0, it.duration)) 
        }
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun toggleRepeat() {
        _playerState.update {
            val newMode = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = newMode)
        }
    }

    fun removeFromQueue(index: Int) {
        _playerState.update { state ->
            val newQueue = state.queue.toMutableList().apply { removeAt(index) }
            val newIndex = when {
                newQueue.isEmpty() -> -1
                index < state.currentIndex -> state.currentIndex - 1
                index == state.currentIndex && index >= newQueue.size -> newQueue.lastIndex
                else -> state.currentIndex
            }
            state.copy(
                queue = newQueue,
                currentIndex = newIndex,
                currentSong = if (newIndex >= 0) newQueue[newIndex] else null
            )
        }
    }

    fun moveInQueue(from: Int, to: Int) {
        _playerState.update { state ->
            val newQueue = state.queue.toMutableList().apply {
                val item = removeAt(from)
                add(to, item)
            }
            val newIndex = when (state.currentIndex) {
                from -> to
                in (minOf(from, to)..maxOf(from, to)) -> {
                    if (from < to) state.currentIndex - 1 else state.currentIndex + 1
                }
                else -> state.currentIndex
            }
            state.copy(queue = newQueue, currentIndex = newIndex)
        }
    }

    fun clearQueue() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _playerState.update {
            PlayerState()
        }
    }

    fun release() {
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
    }
    
    /**
     * Open the current YouTube video in the YouTube app.
     * This is a fallback when direct streaming fails due to authentication.
     */
    fun openInYouTubeApp() {
        val videoId = _playerState.value.youtubeVideoId ?: return
        
        // Clear the error since user is handling it
        _playerState.update { 
            it.copy(
                error = null, 
                canOpenInYouTube = false,
                youtubeVideoId = null
            ) 
        }
        
        // Try to open in YouTube app first
        val youtubeAppIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("vnd.youtube:$videoId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(youtubeAppIntent)
            Log.d(TAG, "Opened YouTube app for video: $videoId")
        } catch (e: ActivityNotFoundException) {
            // YouTube app not installed, open in browser
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.youtube.com/watch?v=$videoId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
                Log.d(TAG, "Opened YouTube in browser for video: $videoId")
            } catch (e2: Exception) {
                Log.e(TAG, "Could not open YouTube: ${e2.message}")
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Could not open YouTube", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Extract YouTube video ID from various URL formats.
     */
    private fun extractYouTubeVideoId(sourceId: String?): String? {
        if (sourceId == null) return null
        
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})")
        )
        
        for (pattern in patterns) {
            pattern.find(sourceId)?.groupValues?.get(1)?.let { return it }
        }
        
        // Maybe it's already just the video ID
        if (sourceId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return sourceId
        }
        
        return null
    }
}
