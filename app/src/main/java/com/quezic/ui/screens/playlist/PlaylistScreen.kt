package com.quezic.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.Song
import com.quezic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Long,
    onNavigateBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlist = uiState.playlist
    val songs = uiState.songs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (playlist == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SystemPink)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with back button overlay
                item {
                    Box {
                        PlaylistHeader(
                            name = playlist.name,
                            description = playlist.description,
                            songCount = songs.size,
                            duration = playlist.formattedDuration,
                            coverUrl = playlist.coverUrl,
                            onPlayAll = { if (songs.isNotEmpty()) onPlayAll(songs) },
                            onShuffle = { if (songs.isNotEmpty()) onPlayAll(songs.shuffled()) }
                        )
                        
                        // Top bar overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.3f)
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            
                            Row {
                                IconButton(
                                    onClick = { viewModel.showEditDialog() },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.showDeleteConfirmation() },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Songs
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Gray5),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = Gray3
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No songs in this playlist",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Add songs from search",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray1
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        PlaylistSongItem(
                            song = song,
                            index = index + 1,
                            allPlaylists = uiState.allPlaylists,
                            currentPlaylistId = playlistId,
                            onClick = { onPlaySong(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onAddToPlaylist = { targetPlaylistId -> 
                                viewModel.addSongToPlaylist(song, targetPlaylistId)
                            },
                            onDownload = { viewModel.downloadSong(song) },
                            onDeleteDownload = { viewModel.deleteDownload(song) },
                            onRemoveFromPlaylist = { viewModel.removeSong(song.id) },
                            onDeleteFromLibrary = { viewModel.deleteSongFromLibrary(song) }
                        )
                    }
                }

                // Suggestions section
                if (uiState.suggestions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = "Suggested Songs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Based on songs in this playlist",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray1
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(uiState.suggestions) { suggestion ->
                        SuggestionItem(
                            result = suggestion,
                            onPlay = { onPlaySong(suggestion.toSong()) },
                            onAdd = { viewModel.addSuggestionToPlaylist(suggestion) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }

    // Edit Dialog
    if (uiState.showEditDialog && playlist != null) {
        EditPlaylistDialog(
            currentName = playlist.name,
            currentDescription = playlist.description ?: "",
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { name, description ->
                viewModel.updatePlaylist(name, description)
            }
        )
    }

    // Delete Confirmation
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmation() },
            containerColor = Gray5,
            title = { 
                Text(
                    "Delete Playlist",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to delete this playlist? This action cannot be undone.",
                    color = Gray1
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist()
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = SystemRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirmation() }) {
                    Text("Cancel", color = Gray1)
                }
            }
        )
    }
}

@Composable
private fun PlaylistHeader(
    name: String,
    description: String?,
    songCount: Int,
    duration: String,
    coverUrl: String?,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Gray5,
                        Color.Black
                    )
                )
            )
            .padding(top = 80.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
    ) {
        // Cover art
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SystemPink, SystemPurple)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray1,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            )
        }

        Text(
            text = "$songCount songs • $duration",
            style = MaterialTheme.typography.bodySmall,
            color = Gray1,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = onShuffle,
                shape = RoundedCornerShape(25.dp),
                color = Gray5,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Shuffle",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Surface(
                onClick = onPlayAll,
                shape = RoundedCornerShape(25.dp),
                color = SystemPink
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Play All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSongItem(
    song: Song,
    index: Int,
    allPlaylists: List<com.quezic.domain.model.Playlist>,
    currentPlaylistId: Long,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onDeleteFromLibrary: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistSubmenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = Gray1,
                modifier = Modifier.width(32.dp)
            )

            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gray5),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.DownloadDone,
                            contentDescription = "Downloaded",
                            modifier = Modifier.size(14.dp),
                            tint = SystemGreen
                        )
                    }
                    if (song.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(14.dp),
                            tint = SystemPink
                        )
                    }
                }
                Text(
                    text = "${song.artist} • ${song.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Rounded.MoreHoriz,
                        contentDescription = "More options",
                        tint = Gray1
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { 
                        showMenu = false 
                        showPlaylistSubmenu = false
                    },
                    modifier = Modifier.background(Gray5)
                ) {
                    // Play
                    DropdownMenuItem(
                        text = { Text("Play", color = Color.White) },
                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, tint = SystemGreen) },
                        onClick = {
                            onClick()
                            showMenu = false
                        }
                    )
                    
                    // Add to Queue
                    DropdownMenuItem(
                        text = { Text("Add to queue", color = Color.White) },
                        leadingIcon = { Icon(Icons.Rounded.QueueMusic, null, tint = SystemBlue) },
                        onClick = {
                            onAddToQueue()
                            showMenu = false
                        }
                    )
                    
                    // Add to Another Playlist
                    DropdownMenuItem(
                        text = { Text("Add to playlist", color = Color.White) },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = SystemPurple) },
                        trailingIcon = { Icon(Icons.Rounded.ChevronRight, null, tint = Gray2) },
                        onClick = { showPlaylistSubmenu = !showPlaylistSubmenu }
                    )
                    
                    // Playlist submenu
                    if (showPlaylistSubmenu) {
                        allPlaylists
                            .filter { it.id != currentPlaylistId }
                            .forEach { playlist ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "  ${playlist.name}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        ) 
                                    },
                                    onClick = {
                                        onAddToPlaylist(playlist.id)
                                        showMenu = false
                                        showPlaylistSubmenu = false
                                    }
                                )
                            }
                    }
                    
                    HorizontalDivider(color = Gray4, modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Favorite toggle
                    DropdownMenuItem(
                        text = { 
                            Text(
                                if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                                color = Color.White
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                if (song.isFavorite) Icons.Rounded.HeartBroken else Icons.Rounded.Favorite,
                                null,
                                tint = SystemPink
                            )
                        },
                        onClick = {
                            onToggleFavorite()
                            showMenu = false
                        }
                    )
                    
                    // Download / Delete Download
                    if (!song.isDownloaded) {
                        DropdownMenuItem(
                            text = { Text("Download", color = Color.White) },
                            leadingIcon = { Icon(Icons.Rounded.Download, null, tint = SystemBlue) },
                            onClick = {
                                onDownload()
                                showMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Delete download", color = SystemOrange) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteForever, null, tint = SystemOrange) },
                            onClick = {
                                onDeleteDownload()
                                showMenu = false
                            }
                        )
                    }
                    
                    HorizontalDivider(color = Gray4, modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Remove from this playlist
                    DropdownMenuItem(
                        text = { Text("Remove from playlist", color = SystemOrange) },
                        leadingIcon = { 
                            Icon(Icons.Rounded.RemoveCircleOutline, null, tint = SystemOrange) 
                        },
                        onClick = {
                            onRemoveFromPlaylist()
                            showMenu = false
                        }
                    )
                    
                    // Delete from library
                    DropdownMenuItem(
                        text = { Text("Delete from library", color = SystemRed) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = SystemRed) },
                        onClick = {
                            onDeleteFromLibrary()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    result: SearchResult,
    onPlay: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlay,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = result.thumbnailUrl,
                contentDescription = result.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gray5),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Rounded.AddCircle,
                    contentDescription = "Add to playlist",
                    tint = SystemPink,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun EditPlaylistDialog(
    currentName: String,
    currentDescription: String,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var description by remember { mutableStateOf(currentDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Gray5,
        title = { 
            Text(
                "Edit Playlist",
                fontWeight = FontWeight.Bold,
                color = Color.White
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemPink,
                        focusedLabelColor = SystemPink,
                        cursorColor = SystemPink,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemPink,
                        focusedLabelColor = SystemPink,
                        cursorColor = SystemPink,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, description.ifBlank { null })
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = SystemPink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray1)
            }
        }
    )
}
