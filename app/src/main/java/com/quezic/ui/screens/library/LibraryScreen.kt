package com.quezic.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.quezic.domain.model.Album
import com.quezic.domain.model.Artist
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.Song
import com.quezic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToImportSpotify: () -> Unit,
    onNavigateToImportSoundCloud: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlaySongInContext: (List<Song>, Int) -> Unit,
    onAddToQueue: (Song) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Artists", "Albums", "Playlists")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button
                Surface(
                    onClick = onNavigateToSettings,
                    shape = CircleShape,
                    color = Gray5
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.padding(12.dp),
                        tint = Color.White
                    )
                }
                
                if (selectedTab == 3) {
                    Surface(
                        onClick = { viewModel.showCreatePlaylistDialog() },
                        shape = CircleShape,
                        color = Gray5
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Create playlist",
                            modifier = Modifier.padding(12.dp),
                            tint = AccentGreen
                        )
                    }
                }
            }
        }

        // iOS-style Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Gray5)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Surface(
                    onClick = { selectedTab = index },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedTab == index) Gray4 else Color.Transparent
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == index) Color.White else Gray1,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                        maxLines = 1
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when (selectedTab) {
            0 -> SongsTab(
                songs = uiState.songs,
                playlists = uiState.playlists,
                onPlaySongInContext = onPlaySongInContext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = viewModel::addSongToPlaylist,
                onDownload = viewModel::downloadSong,
                onDeleteDownload = viewModel::deleteDownload,
                onDelete = viewModel::deleteSong
            )
            1 -> ArtistsTab(
                artists = uiState.artists,
                onArtistClick = onNavigateToArtist
            )
            2 -> AlbumsTab(
                albums = uiState.albums,
                onAlbumClick = onNavigateToAlbum
            )
            3 -> PlaylistsTab(
                playlists = uiState.playlists,
                onPlaylistClick = onNavigateToPlaylist,
                onCreatePlaylist = { viewModel.showCreatePlaylistDialog() },
                onImportSpotify = onNavigateToImportSpotify,
                onImportSoundCloud = onNavigateToImportSoundCloud
            )
        }
    }

    // Create Playlist Dialog
    if (uiState.showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.hideCreatePlaylistDialog() },
            onCreate = { name, description ->
                viewModel.createPlaylist(name, description)
            }
        )
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    playlists: List<Playlist>,
    onPlaySongInContext: (List<Song>, Int) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song, Long) -> Unit,
    onDownload: (Song) -> Unit,
    onDeleteDownload: (Song) -> Unit,
    onDelete: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.MusicNote,
            title = "No songs yet",
            subtitle = "Add songs from the search tab"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${songs.size} songs",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gray1
                    )
                }
            }
            items(songs, key = { it.id }) { song ->
                SongItem(
                    song = song,
                    playlists = playlists,
                    onClick = { onPlaySongInContext(songs, songs.indexOf(song).coerceAtLeast(0)) },
                    onAddToQueue = { onAddToQueue(song) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onAddToPlaylist = { playlistId -> onAddToPlaylist(song, playlistId) },
                    onDownload = { onDownload(song) },
                    onDeleteDownload = { onDeleteDownload(song) },
                    onDelete = { onDelete(song) }
                )
            }
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    playlists: List<Playlist>,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onDelete: () -> Unit
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(52.dp)
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
                            modifier = Modifier.size(16.dp),
                            tint = SystemGreen
                        )
                    }
                    if (song.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(16.dp),
                            tint = AccentGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
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
                    
                    // Add to Playlist submenu
                    DropdownMenuItem(
                        text = { Text("Add to playlist", color = Color.White) },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = SystemPurple) },
                        trailingIcon = { Icon(Icons.Rounded.ChevronRight, null, tint = Gray2) },
                        onClick = { showPlaylistSubmenu = !showPlaylistSubmenu }
                    )
                    
                    // Playlist submenu items
                    if (showPlaylistSubmenu && playlists.isNotEmpty()) {
                        playlists.forEach { playlist ->
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
                    
                    // Favorite
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
                                tint = AccentGreen
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
                    
                    // Delete
                    DropdownMenuItem(
                        text = { Text("Delete from library", color = SystemRed) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = SystemRed) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    onArtistClick: (String) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Person,
            title = "No artists yet",
            subtitle = "Artists will appear when you add songs"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist.name) }
                )
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Gray6
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentGreen, SystemPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (artist.imageUrl != null) {
                    AsyncImage(
                        model = artist.imageUrl,
                        contentDescription = artist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = artist.name.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.songCount} songs",
                style = MaterialTheme.typography.labelSmall,
                color = Gray1
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Album,
            title = "No albums yet",
            subtitle = "Albums will appear when you add songs"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.name) }
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Gray6
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SystemIndigo, SystemPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (album.coverUrl != null) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.Album,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImportSpotify: () -> Unit,
    onImportSoundCloud: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Create new playlist card
            Surface(
                onClick = onCreatePlaylist,
                shape = RoundedCornerShape(12.dp),
                color = Gray6
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Create new playlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        
        item {
            // Import from Spotify card
            Surface(
                onClick = onImportSpotify,
                shape = RoundedCornerShape(12.dp),
                color = Gray6
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1DB954)), // Spotify green
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Import from Spotify",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Transfer your Spotify playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray1
                        )
                    }
                }
            }
        }

        item {
            // Import from SoundCloud card
            Surface(
                onClick = onImportSoundCloud,
                shape = RoundedCornerShape(12.dp),
                color = Gray6
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF5500)), // SoundCloud orange
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Import from SoundCloud",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Transfer your SoundCloud playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray1
                        )
                    }
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playlists yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray1
                    )
                }
            }
        } else {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Gray6
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentGreen, SystemPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverUrl != null) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songCount} songs • ${playlist.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1
                )
            }

            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Gray3
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Gray5),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Gray3
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray1
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Gray5,
        title = { 
            Text(
                "Create Playlist",
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
                        focusedBorderColor = AccentGreen,
                        focusedLabelColor = AccentGreen,
                        cursorColor = AccentGreen,
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
                        focusedBorderColor = AccentGreen,
                        focusedLabelColor = AccentGreen,
                        cursorColor = AccentGreen,
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
                        onCreate(name, description.ifBlank { null })
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create", color = AccentGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray1)
            }
        }
    )
}
