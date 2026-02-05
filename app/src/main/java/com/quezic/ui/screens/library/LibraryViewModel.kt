package com.quezic.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.domain.model.Album
import com.quezic.domain.model.Artist
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.Song
import com.quezic.domain.model.StreamQuality
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import com.quezic.download.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val showCreatePlaylistDialog: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            combine(
                musicRepository.getAllSongs(),
                musicRepository.getAllArtists(),
                musicRepository.getAllAlbums(),
                playlistRepository.getAllPlaylists()
            ) { songs, artists, albums, playlists ->
                LibraryUiState(
                    songs = songs,
                    artists = artists,
                    albums = albums,
                    playlists = playlists
                )
            }.collect { state ->
                _uiState.update { 
                    it.copy(
                        songs = state.songs,
                        artists = state.artists,
                        albums = state.albums,
                        playlists = state.playlists
                    )
                }
            }
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

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            musicRepository.deleteSong(song)
        }
    }
    
    fun addSongToPlaylist(song: Song, playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun showCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = true) }
    }

    fun hideCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }

    fun createPlaylist(name: String, description: String?) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, description)
        }
    }
}
