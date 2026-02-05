package com.quezic.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.Song
import com.quezic.domain.model.SourceType
import com.quezic.ui.components.AddToPlaylistDialog
import com.quezic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onPlaySong: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when song is added to library
    LaunchedEffect(uiState.addedToLibrary) {
        uiState.addedToLibrary?.let { title ->
            snackbarHostState.showSnackbar(
                message = "Added \"$title\" to library",
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Header with Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(20.dp)
        ) {
            Text(
                text = "Search",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // iOS-style Search Bar
            TextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        "Artists, songs, or albums",
                        color = Gray1
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search, 
                        contentDescription = "Search",
                        tint = Gray1
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = uiState.query.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                Icons.Rounded.Cancel, 
                                contentDescription = "Clear",
                                tint = Gray1
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Gray5,
                    unfocusedContainerColor = Gray5,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = SystemPink,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.search()
                        focusManager.clearFocus()
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Source Filter Pills - SoundCloud first (more reliable)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SourcePill(
                        label = "SoundCloud ★",
                        isSelected = SourceType.SOUNDCLOUD in uiState.selectedSources,
                        color = Color(0xFFFF5500),
                        onClick = { viewModel.toggleSource(SourceType.SOUNDCLOUD) }
                    )
                }
                item {
                    SourcePill(
                        label = "YouTube",
                        isSelected = SourceType.YOUTUBE in uiState.selectedSources,
                        color = Color(0xFFFF0000),
                        subtitle = "Limited",
                        onClick = { viewModel.toggleSource(SourceType.YOUTUBE) }
                    )
                }
            }
        }
        
        // Results
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = SystemPink
                    )
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = SystemRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Gray1
                        )
                    }
                }
            }
            
            uiState.query.isEmpty() -> {
                if (uiState.recentSearches.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        item {
                            Text(
                                text = "Recent",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Gray1,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                        items(uiState.recentSearches) { query ->
                            RecentSearchRow(
                                query = query,
                                onClick = {
                                    viewModel.onQueryChange(query)
                                    viewModel.search(query)
                                },
                                onRemove = { viewModel.removeRecentSearch(query) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Gray3
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Find Your Music",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Search by artist, song, or album",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray1
                            )
                        }
                    }
                }
            }
            
            uiState.onlineResults.isEmpty() && uiState.localResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MusicOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Gray3
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "No Results",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Try a different search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray1
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Local results
                    if (uiState.localResults.isNotEmpty()) {
                        item {
                            ResultSectionHeader(
                                title = "In Library",
                                count = uiState.localResults.size
                            )
                        }
                        itemsIndexed(uiState.localResults, key = { index, song -> "local_${song.id}_$index" }) { _, song ->
                            SongResultRow(
                                title = song.title,
                                artist = song.artist,
                                thumbnailUrl = song.thumbnailUrl,
                                duration = song.formattedDuration,
                                sourceType = song.sourceType,
                                isDownloaded = song.isDownloaded,
                                isInLibrary = true,
                                onClick = { onPlaySong(song) },
                                onOptionsClick = { viewModel.showOptionsForSong(song) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                    
                    // Online results
                    if (uiState.onlineResults.isNotEmpty()) {
                        item {
                            ResultSectionHeader(
                                title = "Results",
                                count = uiState.onlineResults.size
                            )
                        }
                        itemsIndexed(uiState.onlineResults, key = { index, result -> "${result.sourceType}_${result.id}_$index" }) { _, result ->
                            SongResultRow(
                                title = result.title,
                                artist = result.artist,
                                thumbnailUrl = result.thumbnailUrl,
                                duration = formatDuration(result.duration),
                                sourceType = result.sourceType,
                                isDownloaded = false,
                                isInLibrary = false,
                                onClick = { onPlaySong(result.toSong()) },
                                onOptionsClick = { viewModel.showOptionsForSearchResult(result) }
                            )
                        }
                    }
                }
            }
        }
        }
        
        // Snackbar host at bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Gray5,
                contentColor = Color.White
            )
        }
    }
    
    // Options Bottom Sheet for songs
    if (uiState.showOptionsMenu) {
        val song = uiState.selectedSong
        val searchResult = uiState.selectedSearchResult
        
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideOptionsMenu() },
            containerColor = Gray5
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Song info header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song?.thumbnailUrl ?: searchResult?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Gray4),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.title ?: searchResult?.title ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song?.artist ?: searchResult?.artist ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Gray4)
                
                // Play option
                OptionsMenuItem(
                    icon = Icons.Rounded.PlayArrow,
                    text = "Play",
                    onClick = {
                        song?.let { onPlaySong(it) }
                        searchResult?.let { onPlaySong(it.toSong()) }
                        viewModel.hideOptionsMenu()
                    }
                )
                
                // Add to Queue
                OptionsMenuItem(
                    icon = Icons.Rounded.QueueMusic,
                    text = "Add to queue",
                    onClick = {
                        song?.let { onAddToQueue(it) }
                        searchResult?.let { onAddToQueue(it.toSong()) }
                        viewModel.hideOptionsMenu()
                    }
                )
                
                // Add to Playlist
                OptionsMenuItem(
                    icon = Icons.Rounded.PlaylistAdd,
                    text = "Add to playlist",
                    onClick = {
                        viewModel.showAddToPlaylistDialog()
                    }
                )
                
                // Add to Library (for search results not in library)
                if (searchResult != null) {
                    OptionsMenuItem(
                        icon = Icons.Rounded.LibraryAdd,
                        text = "Add to library",
                        onClick = {
                            viewModel.addToLibrary(searchResult)
                            viewModel.hideOptionsMenu()
                        }
                    )
                }
                
                // Favorite toggle (only for songs in library)
                if (song != null) {
                    OptionsMenuItem(
                        icon = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        text = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (song.isFavorite) SystemPink else Gray1,
                        onClick = {
                            viewModel.toggleFavorite()
                            viewModel.hideOptionsMenu()
                        }
                    )
                }
            }
        }
    }
    
    // Add to Playlist Dialog
    if (uiState.showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = uiState.playlists,
            songInPlaylists = uiState.songInPlaylists,
            onDismiss = { 
                viewModel.hideAddToPlaylistDialog()
                viewModel.hideOptionsMenu()
            },
            onAddToPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId)
            },
            onRemoveFromPlaylist = { playlistId ->
                viewModel.removeSongFromPlaylist(playlistId)
            },
            onCreateNewPlaylist = {
                // TODO: Navigate to create playlist or show dialog
                viewModel.hideAddToPlaylistDialog()
            }
        )
    }
}

@Composable
private fun OptionsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = Gray1,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SourcePill(
    label: String,
    isSelected: Boolean,
    color: Color,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Gray5
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) color else Gray1
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Gray2
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Gray5
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = SystemPink
            )
        }
    }
}

@Composable
private fun SongResultRow(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    duration: String,
    sourceType: SourceType,
    isDownloaded: Boolean,
    isInLibrary: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with source badge
            Box {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Gray5),
                    contentScale = ContentScale.Crop
                )
                
                // Source indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            when (sourceType) {
                                SourceType.YOUTUBE -> Color(0xFFFF0000)
                                SourceType.SOUNDCLOUD -> Color(0xFFFF5500)
                                else -> SystemBlue
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (sourceType) {
                            SourceType.YOUTUBE -> "Y"
                            SourceType.SOUNDCLOUD -> "S"
                            else -> "L"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.DownloadDone,
                            contentDescription = "Downloaded",
                            modifier = Modifier.size(16.dp),
                            tint = SystemGreen
                        )
                    }
                    if (isInLibrary && !isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "In Library",
                            modifier = Modifier.size(16.dp),
                            tint = SystemPink
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$artist • $duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = Gray1
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = Gray2,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp),
                    tint = Gray2
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
