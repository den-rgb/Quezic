package com.quezic.ui.screens.soundcloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.data.remote.SoundCloudApiService
import com.quezic.data.remote.SoundCloudPlaylist
import com.quezic.data.remote.SoundCloudTrack
import com.quezic.domain.repository.MusicRepository
import com.quezic.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SoundCloudImportStage {
    UrlInput,
    Fetching,
    Review,
    Importing,
    Complete,
    Error
}

data class SoundCloudImportUiState(
    val stage: SoundCloudImportStage = SoundCloudImportStage.UrlInput,
    val soundCloudUrl: String = "",
    val isValidUrl: Boolean = false,
    val playlist: SoundCloudPlaylist? = null,
    val playlistName: String = "",
    val selectedTracks: Set<Int> = emptySet(), // indices of selected tracks
    val importProgress: Float = 0f,
    val error: String? = null,
    val createdPlaylistId: Long? = null
) {
    val selectedCount: Int get() = selectedTracks.size
    val totalTracks: Int get() = playlist?.trackCount ?: 0
    val canImport: Boolean get() = selectedTracks.isNotEmpty() && stage == SoundCloudImportStage.Review
}

@HiltViewModel
class ImportSoundCloudViewModel @Inject constructor(
    private val soundCloudApiService: SoundCloudApiService,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoundCloudImportUiState())
    val uiState: StateFlow<SoundCloudImportUiState> = _uiState.asStateFlow()

    fun onUrlChange(url: String) {
        val isValid = soundCloudApiService.isValidSoundCloudPlaylistUrl(url)
        _uiState.update {
            it.copy(
                soundCloudUrl = url,
                isValidUrl = isValid,
                error = null
            )
        }
    }

    fun startImport() {
        val url = _uiState.value.soundCloudUrl
        if (!_uiState.value.isValidUrl) {
            _uiState.update { it.copy(error = "Please enter a valid SoundCloud playlist URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(stage = SoundCloudImportStage.Fetching, error = null) }

            val result = soundCloudApiService.fetchPlaylist(url)

            result.fold(
                onSuccess = { playlist ->
                    if (playlist.tracks.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                stage = SoundCloudImportStage.Error,
                                error = "No tracks found in this playlist. Make sure it's public and try again."
                            )
                        }
                    } else {
                        // Auto-select all tracks
                        _uiState.update {
                            it.copy(
                                stage = SoundCloudImportStage.Review,
                                playlist = playlist,
                                playlistName = playlist.name,
                                selectedTracks = playlist.tracks.indices.toSet()
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            stage = SoundCloudImportStage.Error,
                            error = "Failed to fetch playlist: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun onPlaylistNameChange(name: String) {
        _uiState.update { it.copy(playlistName = name) }
    }

    fun toggleTrack(index: Int) {
        _uiState.update { state ->
            val newSelected = state.selectedTracks.toMutableSet()
            if (index in newSelected) {
                newSelected.remove(index)
            } else {
                newSelected.add(index)
            }
            state.copy(selectedTracks = newSelected)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedTracks = (0 until state.totalTracks).toSet())
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedTracks = emptySet()) }
    }

    fun importPlaylist() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.canImport) return@launch
            val playlist = state.playlist ?: return@launch

            _uiState.update { it.copy(stage = SoundCloudImportStage.Importing, importProgress = 0f) }

            try {
                val playlistName = state.playlistName.ifBlank { playlist.name }
                val playlistId = playlistRepository.createPlaylist(
                    name = playlistName,
                    description = "Imported from SoundCloud: ${playlist.name}"
                )

                val selectedIndices = state.selectedTracks.sorted()
                val total = selectedIndices.size

                selectedIndices.forEachIndexed { i, trackIndex ->
                    val track = playlist.tracks[trackIndex]
                    val searchResult = soundCloudApiService.trackToSearchResult(track)
                    val song = searchResult.toSong()

                    musicRepository.insertSong(song)
                    playlistRepository.addSongToPlaylist(playlistId, song.id)

                    val progress = (i + 1).toFloat() / total
                    _uiState.update { it.copy(importProgress = progress) }
                }

                _uiState.update {
                    it.copy(
                        stage = SoundCloudImportStage.Complete,
                        createdPlaylistId = playlistId
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        stage = SoundCloudImportStage.Error,
                        error = "Failed to import playlist: ${e.message}"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = SoundCloudImportUiState()
    }

    fun goBack(): Boolean {
        return when (_uiState.value.stage) {
            SoundCloudImportStage.Review -> {
                _uiState.update { it.copy(stage = SoundCloudImportStage.UrlInput) }
                true
            }
            SoundCloudImportStage.Error -> {
                _uiState.update { it.copy(stage = SoundCloudImportStage.UrlInput, error = null) }
                true
            }
            else -> false
        }
    }
}
