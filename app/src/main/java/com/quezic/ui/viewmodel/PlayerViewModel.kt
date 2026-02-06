package com.quezic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.domain.model.PlayerState
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.RepeatMode
import com.quezic.domain.model.Song
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import com.quezic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDialogState(
    val showDialog: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val songInPlaylists: Set<Long> = emptySet(),
    val songToAdd: Song? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val _playlistDialogState = MutableStateFlow(PlaylistDialogState())
    val playlistDialogState: StateFlow<PlaylistDialogState> = _playlistDialogState.asStateFlow()

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
    
    fun toggleFavorite() {
        val currentSong = _playerState.value.currentSong ?: return
        val newFavoriteState = !currentSong.isFavorite
        viewModelScope.launch {
            musicRepository.setFavorite(currentSong.id, newFavoriteState)
            // Update the local state to reflect the change
            _playerState.update { state ->
                state.copy(
                    currentSong = state.currentSong?.copy(isFavorite = newFavoriteState)
                )
            }
        }
    }
    
    fun getCurrentSongShareUrl(): String? {
        return _playerState.value.currentSong?.sourceUrl
    }
    
    // Playlist dialog methods
    fun showAddToPlaylistDialog(song: Song) {
        viewModelScope.launch {
            val playlists = playlistRepository.getAllPlaylists().first()
                .filter { !it.isSmartPlaylist }
            val songInPlaylists = playlistRepository.getPlaylistsContainingSong(song.id)
                .toSet()
            
            _playlistDialogState.update { 
                it.copy(
                    showDialog = true,
                    playlists = playlists,
                    songInPlaylists = songInPlaylists,
                    songToAdd = song
                )
            }
        }
    }
    
    fun hideAddToPlaylistDialog() {
        _playlistDialogState.update { it.copy(showDialog = false, songToAdd = null) }
    }
    
    fun addSongToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val song = _playlistDialogState.value.songToAdd ?: return@launch
            playlistRepository.addSongToPlaylist(playlistId, song.id)
            _playlistDialogState.update { state ->
                state.copy(songInPlaylists = state.songInPlaylists + playlistId)
            }
        }
    }
    
    fun removeSongFromPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val song = _playlistDialogState.value.songToAdd ?: return@launch
            playlistRepository.removeSongFromPlaylist(playlistId, song.id)
            _playlistDialogState.update { state ->
                state.copy(songInPlaylists = state.songInPlaylists - playlistId)
            }
        }
    }
}
