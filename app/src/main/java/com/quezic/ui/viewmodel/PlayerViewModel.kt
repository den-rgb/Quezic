package com.quezic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.domain.model.PlayerState
import com.quezic.domain.model.RepeatMode
import com.quezic.domain.model.Song
import com.quezic.domain.repository.MusicRepository
import com.quezic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        viewModelScope.launch {
            playerController.playerState.collect { state ->
                _playerState.value = state
            }
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            playerController.playSong(song)
            musicRepository.incrementPlayCount(song.id)
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            playerController.playQueue(songs, startIndex)
        }
    }

    fun addToQueue(song: Song) {
        playerController.addToQueue(song)
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun playNext() {
        playerController.playNext()
    }

    fun playPrevious() {
        playerController.playPrevious()
    }

    fun seekTo(position: Long) {
        playerController.seekTo(position)
    }

    fun toggleShuffle() {
        playerController.toggleShuffle()
    }

    fun toggleRepeat() {
        playerController.toggleRepeat()
    }

    fun removeFromQueue(index: Int) {
        playerController.removeFromQueue(index)
    }

    fun moveInQueue(from: Int, to: Int) {
        playerController.moveInQueue(from, to)
    }

    fun clearQueue() {
        playerController.clearQueue()
    }
    
    fun openInYouTubeApp() {
        playerController.openInYouTubeApp()
    }
}
