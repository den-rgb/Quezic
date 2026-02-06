package com.quezic.ui.screens.spotify

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.quezic.domain.model.MatchResult
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.TrackMatchState
import com.quezic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSpotifyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    viewModel: ImportSpotifyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Spotify", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.goBack()) {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.stage) {
                ImportStage.UrlInput -> UrlInputStage(
                    url = uiState.spotifyUrl,
                    isValidUrl = uiState.isValidUrl,
                    error = uiState.error,
                    onUrlChange = viewModel::onUrlChange,
                    onStartImport = viewModel::startImport
                )
                ImportStage.Fetching -> FetchingStage()
                ImportStage.Matching -> MatchingStage(
                    playlist = uiState.spotifyPlaylist,
                    progress = uiState.matchProgress,
                    matchedCount = uiState.matchedCount,
                    totalCount = uiState.totalTracks
                )
                ImportStage.Review -> ReviewStage(
                    playlist = uiState.spotifyPlaylist,
                    playlistName = uiState.playlistName,
                    trackMatches = uiState.trackMatches,
                    matchedCount = uiState.matchedCount,
                    skippedCount = uiState.skippedCount,
                    onPlaylistNameChange = viewModel::onPlaylistNameChange,
                    onSelectMatch = viewModel::selectMatch,
                    onSkipTrack = viewModel::skipTrack,
                    onImport = viewModel::importPlaylist,
                    canImport = uiState.canImport
                )
                ImportStage.Importing -> ImportingStage(
                    progress = uiState.importProgress,
                    matchedCount = uiState.matchedCount
                )
                ImportStage.Complete -> CompleteStage(
                    playlistName = uiState.playlistName,
                    matchedCount = uiState.matchedCount,
                    onViewPlaylist = {
                        uiState.createdPlaylistId?.let { onNavigateToPlaylist(it) }
                    },
                    onDone = onNavigateBack
                )
                ImportStage.Error -> ErrorStage(
                    error = uiState.error ?: "An unknown error occurred",
                    onRetry = viewModel::reset
                )
            }
        }
    }
}

@Composable
private fun UrlInputStage(
    url: String,
    isValidUrl: Boolean,
    error: String?,
    onUrlChange: (String) -> Unit,
    onStartImport: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Spotify icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF1DB954)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Import Spotify Playlist",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Paste a Spotify playlist link to import your songs",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // URL input field
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text("https://open.spotify.com/playlist/...", color = Gray2) 
            },
            leadingIcon = {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = Gray1)
            },
            trailingIcon = {
                IconButton(onClick = {
                    clipboardManager.getText()?.text?.let { onUrlChange(it) }
                }) {
                    Icon(
                        Icons.Rounded.ContentPaste,
                        contentDescription = "Paste",
                        tint = SystemBlue
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Gray5,
                unfocusedContainerColor = Gray5,
                focusedBorderColor = if (isValidUrl) SystemGreen else SystemBlue,
                unfocusedBorderColor = Gray4,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AccentGreen
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (isValidUrl) {
                        focusManager.clearFocus()
                        onStartImport()
                    }
                }
            )
        )

        if (isValidUrl) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SystemGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Valid Spotify playlist URL",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemGreen
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = SystemRed
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartImport,
            enabled = isValidUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1DB954),
                disabledContainerColor = Gray4
            )
        ) {
            Text(
                "Import Playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Gray5),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "How to get a playlist link:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                InstructionStep(1, "Open Spotify and go to your playlist")
                InstructionStep(2, "Tap the 3 dots menu")
                InstructionStep(3, "Select \"Share\" then \"Copy link\"")
                InstructionStep(4, "Paste the link above")
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Note: Only public playlists can be imported",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray2
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Gray4),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = Gray1
        )
    }
}

@Composable
private fun FetchingStage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = Color(0xFF1DB954),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Fetching playlist...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This may take a moment",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1
        )
    }
}

@Composable
private fun MatchingStage(
    playlist: com.quezic.domain.model.SpotifyPlaylist?,
    progress: Float,
    matchedCount: Int,
    totalCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Playlist info
        if (playlist != null) {
            AsyncImage(
                model = playlist.coverUrl,
                contentDescription = playlist.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gray4),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "${playlist.tracks.size} tracks",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray1
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "Finding songs...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = AccentGreen,
            trackColor = Gray4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "$matchedCount of $totalCount tracks matched",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1
        )

        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AccentGreen
        )
    }
}

@Composable
private fun ReviewStage(
    playlist: com.quezic.domain.model.SpotifyPlaylist?,
    playlistName: String,
    trackMatches: List<TrackMatchState>,
    matchedCount: Int,
    skippedCount: Int,
    onPlaylistNameChange: (String) -> Unit,
    onSelectMatch: (Int, SearchResult) -> Unit,
    onSkipTrack: (Int) -> Unit,
    onImport: () -> Unit,
    canImport: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gray6)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playlist?.coverUrl != null) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Gray4),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = onPlaylistNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Playlist name", color = Gray2) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = Gray4,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusChip(
                    icon = Icons.Rounded.CheckCircle,
                    text = "$matchedCount matched",
                    color = SystemGreen
                )
                StatusChip(
                    icon = Icons.Rounded.SkipNext,
                    text = "$skippedCount skipped",
                    color = Gray2
                )
            }
        }

        // Track list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(trackMatches) { index, trackMatch ->
                TrackMatchItem(
                    trackMatch = trackMatch,
                    onSelectMatch = { result -> onSelectMatch(index, result) },
                    onSkip = { onSkipTrack(index) }
                )
            }
        }

        // Import button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Gray6,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = onImport,
                enabled = canImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    disabledContainerColor = Gray4
                )
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Import $matchedCount Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun TrackMatchItem(
    trackMatch: TrackMatchState,
    onSelectMatch: (SearchResult) -> Unit,
    onSkip: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    val spotifyTrack = trackMatch.spotifyTrack
    val matchedResult = trackMatch.displayResult

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (trackMatch.result is MatchResult.MultipleOptions) {
                    showOptions = !showOptions
                }
            },
        color = Color.Transparent
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Match status icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                trackMatch.isMatched -> SystemGreen.copy(alpha = 0.2f)
                                trackMatch.isSkipped -> Gray4
                                else -> SystemOrange.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when {
                            trackMatch.isMatched -> Icons.Rounded.CheckCircle
                            trackMatch.isSkipped -> Icons.Rounded.SkipNext
                            else -> Icons.Rounded.HelpOutline
                        },
                        contentDescription = null,
                        tint = when {
                            trackMatch.isMatched -> SystemGreen
                            trackMatch.isSkipped -> Gray2
                            else -> SystemOrange
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        spotifyTrack.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (trackMatch.isSkipped) Gray2 else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        spotifyTrack.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (matchedResult != null) {
                        Text(
                            "Found on ${matchedResult.sourceType.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemBlue
                        )
                    }
                }

                // Actions
                if (!trackMatch.isSkipped && !trackMatch.isMatched) {
                    IconButton(onClick = onSkip) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Skip",
                            tint = Gray2
                        )
                    }
                }

                // Extract options safely to avoid cast issues
                val hasMultipleOptions = trackMatch.result is MatchResult.MultipleOptions
                if (hasMultipleOptions) {
                    Icon(
                        if (showOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Show options",
                        tint = Gray1
                    )
                }
            }

            // Expandable options for multiple matches
            // Use remember to keep options stable during exit animation
            val multipleOptions = remember(trackMatch.result) {
                (trackMatch.result as? MatchResult.MultipleOptions)?.options ?: emptyList()
            }
            
            AnimatedVisibility(
                visible = showOptions && multipleOptions.isNotEmpty()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray5)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Select the correct match:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gray1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    multipleOptions.forEach { option ->
                        MatchOption(
                            result = option,
                            isSelected = trackMatch.selectedResult?.id == option.id,
                            onClick = { onSelectMatch(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchOption(
    result: SearchResult,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) AccentGreen.copy(alpha = 0.2f) else Gray4
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = result.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Gray3),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${result.artist} - ${result.sourceType.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray1
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ImportingStage(
    progress: Float,
    matchedCount: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(100.dp),
            color = AccentGreen,
            trackColor = Gray4,
            strokeWidth = 8.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Importing songs...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "${(progress * matchedCount).toInt()} of $matchedCount",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1
        )
    }
}

@Composable
private fun CompleteStage(
    playlistName: String,
    matchedCount: Int,
    onViewPlaylist: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SystemGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = SystemGreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Import Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "$matchedCount songs added to \"$playlistName\"",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onViewPlaylist,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "View Playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onDone) {
            Text("Done", color = Gray1)
        }
    }
}

@Composable
private fun ErrorStage(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SystemRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = SystemRed
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Import Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Try Again",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
