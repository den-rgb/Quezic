package com.quezic.ui.screens.soundcloud

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
import com.quezic.data.remote.SoundCloudTrack
import com.quezic.ui.theme.*

private val SoundCloudOrange = Color(0xFFFF5500)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSoundCloudScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    viewModel: ImportSoundCloudViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from SoundCloud", color = Color.White) },
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
                SoundCloudImportStage.UrlInput -> UrlInputStage(
                    url = uiState.soundCloudUrl,
                    isValidUrl = uiState.isValidUrl,
                    error = uiState.error,
                    onUrlChange = viewModel::onUrlChange,
                    onStartImport = viewModel::startImport
                )
                SoundCloudImportStage.Fetching -> FetchingStage()
                SoundCloudImportStage.Review -> ReviewStage(
                    playlist = uiState.playlist,
                    playlistName = uiState.playlistName,
                    selectedTracks = uiState.selectedTracks,
                    selectedCount = uiState.selectedCount,
                    totalTracks = uiState.totalTracks,
                    onPlaylistNameChange = viewModel::onPlaylistNameChange,
                    onToggleTrack = viewModel::toggleTrack,
                    onSelectAll = viewModel::selectAll,
                    onDeselectAll = viewModel::deselectAll,
                    onImport = viewModel::importPlaylist,
                    canImport = uiState.canImport
                )
                SoundCloudImportStage.Importing -> ImportingStage(
                    progress = uiState.importProgress,
                    count = uiState.selectedCount
                )
                SoundCloudImportStage.Complete -> CompleteStage(
                    playlistName = uiState.playlistName,
                    count = uiState.selectedCount,
                    onViewPlaylist = {
                        uiState.createdPlaylistId?.let { onNavigateToPlaylist(it) }
                    },
                    onDone = onNavigateBack
                )
                SoundCloudImportStage.Error -> ErrorStage(
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

        // SoundCloud icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SoundCloudOrange),
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
            text = "Import SoundCloud Playlist",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Paste a SoundCloud playlist link to import your songs",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("https://soundcloud.com/user/sets/playlist", color = Gray2)
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
                cursorColor = SoundCloudOrange
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
                    "Valid SoundCloud playlist URL",
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
                containerColor = SoundCloudOrange,
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
                InstructionStep(1, "Open SoundCloud and go to your playlist")
                InstructionStep(2, "Tap the share button")
                InstructionStep(3, "Select \"Copy link\"")
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
            color = SoundCloudOrange,
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
private fun ReviewStage(
    playlist: com.quezic.data.remote.SoundCloudPlaylist?,
    playlistName: String,
    selectedTracks: Set<Int>,
    selectedCount: Int,
    totalTracks: Int,
    onPlaylistNameChange: (String) -> Unit,
    onToggleTrack: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
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
                            focusedBorderColor = SoundCloudOrange,
                            unfocusedBorderColor = Gray4,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$selectedCount of $totalTracks tracks selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1
                )
                Row {
                    TextButton(onClick = onSelectAll) {
                        Text("Select all", color = SoundCloudOrange, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDeselectAll) {
                        Text("Deselect all", color = Gray2, fontSize = 12.sp)
                    }
                }
            }
        }

        // Track list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val tracks = playlist?.tracks ?: emptyList()
            itemsIndexed(tracks) { index, track ->
                TrackItem(
                    track = track,
                    isSelected = index in selectedTracks,
                    onToggle = { onToggleTrack(index) }
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
                    containerColor = SoundCloudOrange,
                    disabledContainerColor = Gray4
                )
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Import $selectedCount Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: SoundCloudTrack,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SoundCloudOrange,
                    uncheckedColor = Gray2
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Gray4),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else Gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                track.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = Gray2
            )
        }
    }
}

@Composable
private fun ImportingStage(
    progress: Float,
    count: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(100.dp),
            color = SoundCloudOrange,
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
            "${(progress * count).toInt()} of $count",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1
        )
    }
}

@Composable
private fun CompleteStage(
    playlistName: String,
    count: Int,
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
            "$count songs added to \"$playlistName\"",
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
            colors = ButtonDefaults.buttonColors(containerColor = SoundCloudOrange)
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
            colors = ButtonDefaults.buttonColors(containerColor = SoundCloudOrange)
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
