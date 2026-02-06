package com.quezic.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.Song
import com.quezic.domain.model.SourceType
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val onlineResults: List<SearchResult> = emptyList(),
    val localResults: List<Song> = emptyList(),
    // SoundCloud first as it's more reliable currently
    val selectedSources: Set<SourceType> = setOf(SourceType.SOUNDCLOUD, SourceType.YOUTUBE),
    val error: String? = null,
    val recentSearches: List<String> = emptyList(),
    val addedToLibrary: String? = null, // For showing feedback
    val youtubeWarningShown: Boolean = false,
    // Options menu state
    val selectedSong: Song? = null,
    val selectedSearchResult: SearchResult? = null,
    val showOptionsMenu: Boolean = false,
    val showAddToPlaylistDialog: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val songInPlaylists: Set<Long> = emptySet()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    
    init {
        // Load playlists for the add to playlist dialog
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        
        // Debounced search
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { 
                it.copy(
                    onlineResults = emptyList(),
                    localResults = emptyList(),
                    isLoading = false
                ) 
            }
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            search(query)
        }
    }

    fun search(query: String = _uiState.value.query) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Search local library
                val localJob = launch {
                    musicRepository.searchLocalSongs(query).collect { songs ->
                        _uiState.update { it.copy(localResults = songs) }
                    }
                }
                
                // Search online sources
                val onlineResults = musicRepository.searchOnline(
                    query = query,
                    sources = _uiState.value.selectedSources.toList()
                )
                
                _uiState.update { 
                    it.copy(
                        onlineResults = onlineResults,
                        isLoading = false
                    ) 
                }
                
                // Save to recent searches
                saveRecentSearch(query)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Search failed"
                    ) 
                }
            }
        }
    }

    fun toggleSource(source: SourceType) {
        _uiState.update { state ->
            val newSources = if (source in state.selectedSources) {
                // Don't allow deselecting all sources
                if (state.selectedSources.size > 1) {
                    state.selectedSources - source
                } else {
                    state.selectedSources
                }
            } else {
                state.selectedSources + source
            }
            state.copy(selectedSources = newSources)
        }
        
        // Re-search with new sources
        if (_uiState.value.query.isNotBlank()) {
            search()
        }
    }

    fun addToLibrary(result: SearchResult) {
        viewModelScope.launch {
            val song = result.toSong()
            musicRepository.insertSong(song)
            
            // Optimistic update: immediately add to local results AND remove from online results
            // This ensures the song appears immediately without waiting for database Flow
            _uiState.update { state ->
                val updatedLocalResults = if (state.localResults.none { it.id == song.id }) {
                    listOf(song) + state.localResults // Add to top of local results
                } else {
                    state.localResults
                }
                state.copy(
                    onlineResults = state.onlineResults.filter { it.id != result.id },
                    localResults = updatedLocalResults,
                    addedToLibrary = result.title
                )
            }
            
            // Clear feedback after delay
            delay(2000)
            _uiState.update { it.copy(addedToLibrary = null) }
        }
    }

    fun clearSearch() {
        _uiState.update { 
            it.copy(
                query = "",
                onlineResults = emptyList(),
                localResults = emptyList()
            ) 
        }
    }

    private fun saveRecentSearch(query: String) {
        _uiState.update { state ->
            val updated = (listOf(query) + state.recentSearches)
                .distinct()
                .take(10)
            state.copy(recentSearches = updated)
        }
    }

    fun removeRecentSearch(query: String) {
        _uiState.update { state ->
            state.copy(recentSearches = state.recentSearches - query)
        }
    }
    
    // Options menu functions
    fun showOptionsForSong(song: Song) {
        viewModelScope.launch {
            // Get which playlists this song is in
            val playlistIds = playlistRepository.getPlaylistsContainingSong(song.id)
            _uiState.update { 
                it.copy(
                    selectedSong = song,
                    selectedSearchResult = null,
                    showOptionsMenu = true,
                    songInPlaylists = playlistIds.toSet()
                )
            }
        }
    }
    
    fun showOptionsForSearchResult(result: SearchResult) {
        _uiState.update { 
            it.copy(
                selectedSearchResult = result,
                selectedSong = null,
                showOptionsMenu = true
            )
        }
    }
    
    fun hideOptionsMenu() {
        _uiState.update { 
            it.copy(
                showOptionsMenu = false,
                selectedSong = null,
                selectedSearchResult = null
            )
        }
    }
    
    fun showAddToPlaylistDialog() {
        _uiState.update { it.copy(showAddToPlaylistDialog = true) }
    }
    
    fun hideAddToPlaylistDialog() {
        _uiState.update { it.copy(showAddToPlaylistDialog = false) }
    }
    
    fun addSongToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val selectedSearchResult = _uiState.value.selectedSearchResult
            val song = _uiState.value.selectedSong ?: selectedSearchResult?.toSong()
            song?.let {
                // Make sure song is in library first
                if (selectedSearchResult != null) {
                    musicRepository.insertSong(it)
                    // Optimistic update: add to local results and remove from online results
                    _uiState.update { state ->
                        val updatedLocalResults = if (state.localResults.none { existing -> existing.id == it.id }) {
                            listOf(it) + state.localResults
                        } else {
                            state.localResults
                        }
                        state.copy(
                            onlineResults = state.onlineResults.filter { result -> result.id != selectedSearchResult.id },
                            localResults = updatedLocalResults
                        )
                    }
                }
                playlistRepository.addSongToPlaylist(playlistId, it.id)
                _uiState.update { state ->
                    state.copy(
                        songInPlaylists = state.songInPlaylists + playlistId,
                        addedToLibrary = "Added to playlist"
                    )
                }
            }
            delay(1500)
            _uiState.update { it.copy(addedToLibrary = null) }
        }
    }
    
    fun removeSongFromPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val songId = _uiState.value.selectedSong?.id 
                ?: _uiState.value.selectedSearchResult?.id
            songId?.let {
                playlistRepository.removeSongFromPlaylist(playlistId, it)
                _uiState.update { state ->
                    state.copy(songInPlaylists = state.songInPlaylists - playlistId)
                }
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, description: String) {
        viewModelScope.launch {
            val selectedSearchResult = _uiState.value.selectedSearchResult
            val song = _uiState.value.selectedSong ?: selectedSearchResult?.toSong()
            song?.let {
                // Make sure song is in library first
                if (selectedSearchResult != null) {
                    musicRepository.insertSong(it)
                }
                val playlistId = playlistRepository.createPlaylist(name, description)
                playlistRepository.addSongToPlaylist(playlistId, it.id)
                _uiState.update { state ->
                    state.copy(
                        showAddToPlaylistDialog = false,
                        addedToLibrary = "Added to \"$name\""
                    )
                }
            }
            hideOptionsMenu()
            delay(1500)
            _uiState.update { it.copy(addedToLibrary = null) }
        }
    }
    
    fun toggleFavorite() {
        viewModelScope.launch {
            val song = _uiState.value.selectedSong
            song?.let {
                val newFavoriteState = !it.isFavorite
                musicRepository.setFavorite(it.id, newFavoriteState)
                // Optimistic update: immediately update the song's favorite state in local results
                _uiState.update { state ->
                    state.copy(
                        localResults = state.localResults.map { existing ->
                            if (existing.id == it.id) {
                                existing.copy(isFavorite = newFavoriteState)
                            } else {
                                existing
                            }
                        }
                    )
                }
            }
        }
    }
}
