package com.quezic.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.Song
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import com.quezic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentlyPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine all data flows
            combine(
                musicRepository.getRecentlyPlayedSongs(10),
                musicRepository.getRecentlyAddedSongs(10),
                musicRepository.getMostPlayedSongs(10),
                musicRepository.getFavoriteSongs(),
                playlistRepository.getAllPlaylists()
            ) { recentlyPlayed, recentlyAdded, mostPlayed, favorites, playlists ->
                HomeUiState(
                    recentlyPlayed = recentlyPlayed,
                    recentlyAdded = recentlyAdded,
                    mostPlayed = mostPlayed,
                    favorites = favorites,
                    playlists = playlists
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun playFavorites() {
        val favorites = _uiState.value.favorites
        if (favorites.isNotEmpty()) {
            playerController.playQueue(favorites)
        }
    }

    fun playRecentlyPlayed() {
        val recent = _uiState.value.recentlyPlayed
        if (recent.isNotEmpty()) {
            playerController.playQueue(recent)
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            musicRepository.getAllSongs().first().let { songs ->
                if (songs.isNotEmpty()) {
                    playerController.playQueue(songs.shuffled())
                }
            }
        }
    }
}
