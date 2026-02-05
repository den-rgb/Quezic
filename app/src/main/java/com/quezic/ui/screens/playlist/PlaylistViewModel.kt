package com.quezic.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.Song
import com.quezic.domain.model.StreamQuality
import com.quezic.domain.recommendation.RecommendationEngine
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import com.quezic.download.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val suggestions: List<SearchResult> = emptyList(),
    val allPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val showEditDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    private val recommendationEngine: RecommendationEngine,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private var currentPlaylistId: Long = 0
    
    init {
        // Load all playlists for "Add to playlist" feature
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(allPlaylists = playlists) }
            }
        }
    }

    fun loadPlaylist(playlistId: Long) {
        currentPlaylistId = playlistId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Observe playlist changes
            combine(
                playlistRepository.getPlaylistById(playlistId),
                playlistRepository.getSongsInPlaylist(playlistId)
            ) { playlist, songs ->
                Pair(playlist, songs)
            }.collect { (playlist, songs) ->
                _uiState.update {
                    it.copy(
                        playlist = playlist,
                        songs = songs,
                        isLoading = false
                    )
                }
                
                // Load suggestions based on playlist songs
                if (songs.isNotEmpty()) {
                    loadSuggestions(songs)
                }
            }
        }
    }

    private fun loadSuggestions(songs: List<Song>) {
        viewModelScope.launch {
            try {
                val suggestions = recommendationEngine.getRecommendations(songs, limit = 5)
                _uiState.update { it.copy(suggestions = suggestions) }
            } catch (e: Exception) {
                // Silently fail for suggestions
            }
        }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(currentPlaylistId, songId)
        }
    }

    fun addSuggestionToPlaylist(result: SearchResult) {
        viewModelScope.launch {
            // First add to library
            val song = result.toSong()
            musicRepository.insertSong(song)
            
            // Then add to playlist
            playlistRepository.addSongToPlaylist(currentPlaylistId, song.id)
            
            // Remove from suggestions
            _uiState.update { state ->
                state.copy(suggestions = state.suggestions.filter { it.id != result.id })
            }
        }
    }

    fun showEditDialog() {
        _uiState.update { it.copy(showEditDialog = true) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false) }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun updatePlaylist(name: String, description: String?) {
        viewModelScope.launch {
            _uiState.value.playlist?.let { playlist ->
                playlistRepository.updatePlaylist(
                    playlist.copy(
                        name = name,
                        description = description,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(currentPlaylistId)
        }
    }

    fun reorderSongs(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentSongs = _uiState.value.songs.toMutableList()
            val song = currentSongs.removeAt(fromIndex)
            currentSongs.add(toIndex, song)
            
            playlistRepository.reorderSongs(
                currentPlaylistId,
                currentSongs.map { it.id }
            )
        }
    }
    
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.setFavorite(song.id, !song.isFavorite)
        }
    }
    
    fun downloadSong(song: Song) {
        downloadManager.downloadSong(song, StreamQuality.HIGH)
    }
    
    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            musicRepository.deleteDownload(song)
        }
    }
    
    fun addSongToPlaylist(song: Song, playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song.id)
        }
    }
    
    fun deleteSongFromLibrary(song: Song) {
        viewModelScope.launch {
            musicRepository.deleteSong(song)
        }
    }
}
