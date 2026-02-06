package com.quezic.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.domain.model.DownloadState
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

data class DownloadProgress(
    val totalSongs: Int = 0,
    val completedSongs: Int = 0,
    val failedSongs: Int = 0,
    val currentlyDownloading: Int = 0,
    val isActive: Boolean = false
)

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val suggestions: List<SearchResult> = emptyList(),
    val allPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingSuggestions: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val pendingCoverUri: String? = null,
    val requestImagePick: Boolean = false,
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val songDownloadStates: Map<String, DownloadState> = emptyMap()
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
    
    private var downloadingSongIds: Set<String> = emptySet()
    
    // Track whether suggestions have been loaded to avoid re-fetching on every flow emission
    private var suggestionsLoaded: Boolean = false
    
    init {
        // Load all playlists for "Add to playlist" feature
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(allPlaylists = playlists) }
            }
        }
        
        // Observe download states
        viewModelScope.launch {
            downloadManager.downloads.collect { downloads ->
                updateDownloadProgress(downloads)
                // Update per-song download states
                _uiState.update { state ->
                    state.copy(songDownloadStates = downloads.mapValues { it.value.state })
                }
            }
        }
    }
    
    private fun updateDownloadProgress(downloads: Map<String, com.quezic.domain.model.DownloadItem>) {
        val relevantDownloads = downloads.filterKeys { it in downloadingSongIds }
        
        if (relevantDownloads.isEmpty() && downloadingSongIds.isEmpty()) {
            _uiState.update { it.copy(downloadProgress = DownloadProgress()) }
            return
        }
        
        val completed = relevantDownloads.count { it.value.state is DownloadState.Completed }
        val failed = relevantDownloads.count { it.value.state is DownloadState.Failed }
        val downloading = relevantDownloads.count { 
            it.value.state is DownloadState.Downloading || it.value.state is DownloadState.Queued 
        }
        
        val isActive = downloading > 0
        
        // Clear tracking when all done
        if (!isActive && downloadingSongIds.isNotEmpty()) {
            // Keep showing for a moment so user sees completion
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                if (downloadManager.downloads.value.filterKeys { it in downloadingSongIds }
                    .none { it.value.state is DownloadState.Downloading || it.value.state is DownloadState.Queued }) {
                    downloadingSongIds = emptySet()
                    _uiState.update { it.copy(downloadProgress = DownloadProgress()) }
                }
            }
        }
        
        _uiState.update { 
            it.copy(
                downloadProgress = DownloadProgress(
                    totalSongs = downloadingSongIds.size,
                    completedSongs = completed,
                    failedSongs = failed,
                    currentlyDownloading = downloading,
                    isActive = isActive || downloadingSongIds.isNotEmpty()
                )
            )
        }
    }

    fun loadPlaylist(playlistId: Long) {
        currentPlaylistId = playlistId
        suggestionsLoaded = false
        
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
                
                // Only load suggestions once per playlist load, not on every flow emission
                if (songs.isNotEmpty() && !suggestionsLoaded) {
                    suggestionsLoaded = true
                    loadSuggestions(songs)
                }
            }
        }
    }

    private fun loadSuggestions(songs: List<Song>, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingSuggestions = true) }
                val suggestions = recommendationEngine.getRecommendations(songs, limit = 5, forceRefresh = forceRefresh)
                _uiState.update { it.copy(suggestions = suggestions, isLoadingSuggestions = false) }
            } catch (e: Exception) {
                // Silently fail for suggestions
                _uiState.update { it.copy(isLoadingSuggestions = false) }
            }
        }
    }
    
    fun refreshSuggestions() {
        val songs = _uiState.value.songs
        if (songs.isNotEmpty()) {
            loadSuggestions(songs, forceRefresh = true)
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
        _uiState.update { it.copy(showEditDialog = true, pendingCoverUri = null) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, pendingCoverUri = null, requestImagePick = false) }
    }
    
    fun requestImagePick() {
        _uiState.update { it.copy(requestImagePick = true) }
    }
    
    fun onImagePickHandled() {
        _uiState.update { it.copy(requestImagePick = false) }
    }
    
    fun onImagePicked(uri: String?) {
        _uiState.update { it.copy(pendingCoverUri = uri, requestImagePick = false) }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun updatePlaylist(name: String, description: String?, coverUrl: String? = null) {
        viewModelScope.launch {
            _uiState.value.playlist?.let { playlist ->
                playlistRepository.updatePlaylist(
                    playlist.copy(
                        name = name,
                        description = description,
                        coverUrl = coverUrl ?: playlist.coverUrl,
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
    
    fun downloadAllSongs() {
        val songs = _uiState.value.songs
        val songsToDownload = songs.filter { !it.isDownloaded }
        
        if (songsToDownload.isEmpty()) return
        
        // Track which songs we're downloading
        downloadingSongIds = songsToDownload.map { it.id }.toSet()
        
        // Start all downloads
        songsToDownload.forEach { song ->
            downloadManager.downloadSong(song, StreamQuality.HIGH)
        }
    }
    
    fun getNotDownloadedCount(): Int {
        return _uiState.value.songs.count { !it.isDownloaded }
    }
    
    fun getDownloadState(songId: String): DownloadState {
        return downloadManager.getDownloadState(songId)
    }
    
    fun getSongDownloadStates(): Map<String, DownloadState> {
        return downloadManager.downloads.value.mapValues { it.value.state }
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
