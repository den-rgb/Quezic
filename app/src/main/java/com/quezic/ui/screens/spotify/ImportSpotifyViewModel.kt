package com.quezic.ui.screens.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.data.remote.SpotifyApiService
import com.quezic.domain.model.MatchResult
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.SpotifyPlaylist
import com.quezic.domain.model.TrackMatchState
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import com.quezic.domain.service.SongMatcherService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Import stages for the Spotify playlist import flow.
 */
enum class ImportStage {
    UrlInput,      // User enters Spotify URL
    Fetching,      // Fetching playlist data from Spotify
    Matching,      // Matching tracks to YouTube/SoundCloud
    Review,        // User reviews and adjusts matches
    Importing,     // Creating playlist and adding songs
    Complete,      // Import successful
    Error          // Something went wrong
}

/**
 * UI state for the Spotify import screen.
 */
data class ImportUiState(
    val stage: ImportStage = ImportStage.UrlInput,
    val spotifyUrl: String = "",
    val isValidUrl: Boolean = false,
    val spotifyPlaylist: SpotifyPlaylist? = null,
    val trackMatches: List<TrackMatchState> = emptyList(),
    val matchProgress: Float = 0f,
    val importProgress: Float = 0f,
    val error: String? = null,
    val createdPlaylistId: Long? = null,
    val playlistName: String = ""
) {
    val matchedCount: Int
        get() = trackMatches.count { it.isMatched }
    
    val skippedCount: Int
        get() = trackMatches.count { it.isSkipped }
    
    val unmatchedCount: Int
        get() = trackMatches.count { !it.isMatched && !it.isSkipped && !it.isProcessing }
    
    val totalTracks: Int
        get() = trackMatches.size
    
    val canImport: Boolean
        get() = matchedCount > 0 && stage == ImportStage.Review
}

@HiltViewModel
class ImportSpotifyViewModel @Inject constructor(
    private val spotifyApiService: SpotifyApiService,
    private val songMatcherService: SongMatcherService,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    /**
     * Updates the Spotify URL and validates it.
     */
    fun onUrlChange(url: String) {
        val isValid = spotifyApiService.isValidSpotifyPlaylistUrl(url)
        _uiState.update { 
            it.copy(
                spotifyUrl = url, 
                isValidUrl = isValid,
                error = null
            ) 
        }
    }

    /**
     * Starts the import process by fetching the playlist.
     */
    fun startImport() {
        val url = _uiState.value.spotifyUrl
        if (!_uiState.value.isValidUrl) {
            _uiState.update { it.copy(error = "Please enter a valid Spotify playlist URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(stage = ImportStage.Fetching, error = null) }

            val result = spotifyApiService.fetchPlaylist(url)

            result.fold(
                onSuccess = { playlist ->
                    if (playlist.tracks.isEmpty()) {
                        _uiState.update { 
                            it.copy(
                                stage = ImportStage.Error,
                                error = "Could not fetch tracks from this playlist. Make sure it's public and try again."
                            ) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                spotifyPlaylist = playlist,
                                playlistName = playlist.name
                            ) 
                        }
                        startMatching(playlist)
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            stage = ImportStage.Error,
                            error = "Failed to fetch playlist: ${error.message}"
                        ) 
                    }
                }
            )
        }
    }

    /**
     * Starts matching Spotify tracks to local sources.
     */
    private fun startMatching(playlist: SpotifyPlaylist) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    stage = ImportStage.Matching,
                    trackMatches = playlist.tracks.map { track ->
                        TrackMatchState(spotifyTrack = track, isProcessing = true)
                    },
                    matchProgress = 0f
                ) 
            }

            val matches = songMatcherService.matchTracks(
                tracks = playlist.tracks,
                onProgress = { progress ->
                    _uiState.update { it.copy(matchProgress = progress) }
                },
                onTrackMatched = { index, state ->
                    _uiState.update { current ->
                        val updatedMatches = current.trackMatches.toMutableList()
                        if (index < updatedMatches.size) {
                            updatedMatches[index] = state
                        }
                        current.copy(trackMatches = updatedMatches)
                    }
                }
            )

            _uiState.update { 
                it.copy(
                    stage = ImportStage.Review,
                    trackMatches = matches
                ) 
            }
        }
    }

    /**
     * Selects a specific search result for a track.
     */
    fun selectMatch(trackIndex: Int, result: SearchResult) {
        _uiState.update { current ->
            val updatedMatches = current.trackMatches.toMutableList()
            if (trackIndex < updatedMatches.size) {
                updatedMatches[trackIndex] = updatedMatches[trackIndex].copy(
                    selectedResult = result,
                    result = MatchResult.Matched(result, 1f)
                )
            }
            current.copy(trackMatches = updatedMatches)
        }
    }

    /**
     * Skips a track (won't be imported).
     */
    fun skipTrack(trackIndex: Int) {
        _uiState.update { current ->
            val updatedMatches = current.trackMatches.toMutableList()
            if (trackIndex < updatedMatches.size) {
                updatedMatches[trackIndex] = updatedMatches[trackIndex].copy(
                    result = MatchResult.Skipped,
                    selectedResult = null
                )
            }
            current.copy(trackMatches = updatedMatches)
        }
    }

    /**
     * Updates the playlist name.
     */
    fun onPlaylistNameChange(name: String) {
        _uiState.update { it.copy(playlistName = name) }
    }

    /**
     * Imports the matched tracks as a new playlist.
     */
    fun importPlaylist() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.canImport) return@launch

            _uiState.update { it.copy(stage = ImportStage.Importing, importProgress = 0f) }

            try {
                // Create the playlist
                val playlistName = state.playlistName.ifBlank { 
                    state.spotifyPlaylist?.name ?: "Imported Playlist" 
                }
                val playlistId = playlistRepository.createPlaylist(
                    name = playlistName,
                    description = "Imported from Spotify: ${state.spotifyPlaylist?.name}"
                )

                // Get matched tracks
                val matchedTracks = state.trackMatches.filter { it.isMatched }
                val total = matchedTracks.size

                matchedTracks.forEachIndexed { index, trackMatch ->
                    val searchResult = trackMatch.displayResult
                    if (searchResult != null) {
                        // Add song to library first
                        val song = searchResult.toSong()
                        musicRepository.insertSong(song)

                        // Add to playlist
                        playlistRepository.addSongToPlaylist(playlistId, song.id)
                    }

                    val progress = (index + 1).toFloat() / total
                    _uiState.update { it.copy(importProgress = progress) }
                }

                _uiState.update { 
                    it.copy(
                        stage = ImportStage.Complete,
                        createdPlaylistId = playlistId
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        stage = ImportStage.Error,
                        error = "Failed to import playlist: ${e.message}"
                    ) 
                }
            }
        }
    }

    /**
     * Resets the import flow to start over.
     */
    fun reset() {
        _uiState.value = ImportUiState()
    }

    /**
     * Goes back to the previous stage.
     */
    fun goBack(): Boolean {
        val currentStage = _uiState.value.stage
        return when (currentStage) {
            ImportStage.Review -> {
                _uiState.update { it.copy(stage = ImportStage.UrlInput) }
                true
            }
            ImportStage.Error -> {
                _uiState.update { it.copy(stage = ImportStage.UrlInput, error = null) }
                true
            }
            else -> false
        }
    }
}
