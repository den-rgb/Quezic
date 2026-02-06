package com.quezic.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.quezic.domain.model.Playlist
import com.quezic.domain.model.Song
import com.quezic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onPlaySong: (Song) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Header
        item {
            HomeHeader(onNavigateToSearch = onNavigateToSearch)
        }
        
        // Quick Actions
        item {
            QuickActionRow(
                onPlayFavorites = { viewModel.playFavorites() },
                onPlayRecent = { viewModel.playRecentlyPlayed() },
                onShuffleAll = { viewModel.shuffleAll() }
            )
        }
        
        // Recently Played
        if (uiState.recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Played")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.recentlyPlayed) { song ->
                        LargeSongCard(
                            song = song,
                            onClick = { onPlaySong(song) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // Playlists
        if (uiState.playlists.isNotEmpty()) {
            item {
                SectionHeader(title = "Your Playlists")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.playlists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onNavigateToPlaylist(playlist.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // Most Played
        if (uiState.mostPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Top Tracks")
            }
            items(uiState.mostPlayed.take(5)) { song ->
                CompactSongRow(
                    song = song,
                    onClick = { onPlaySong(song) },
                    showPlayCount = true
                )
            }
        }
        
        // Recently Added
        if (uiState.recentlyAdded.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Added")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.recentlyAdded) { song ->
                        LargeSongCard(
                            song = song,
                            onClick = { onPlaySong(song) }
                        )
                    }
                }
            }
        }
        
        // Empty state
        if (uiState.recentlyPlayed.isEmpty() && uiState.playlists.isEmpty() && 
            uiState.mostPlayed.isEmpty() && uiState.recentlyAdded.isEmpty()) {
            item {
                EmptyLibraryState(onNavigateToSearch = onNavigateToSearch)
            }
        }
    }
}

@Composable
private fun HomeHeader(onNavigateToSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = getGreeting(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Surface(
                onClick = onNavigateToSearch,
                shape = CircleShape,
                color = Gray5
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Search",
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onPlayFavorites: () -> Unit,
    onPlayRecent: () -> Unit,
    onShuffleAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Favorite,
            title = "Liked",
            color = AccentGreen,
            onClick = onPlayFavorites
        )
        QuickActionPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Schedule,
            title = "Recent",
            color = SystemBlue,
            onClick = onPlayRecent
        )
        QuickActionPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Shuffle,
            title = "Mix",
            color = SystemGreen,
            onClick = onShuffleAll
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun QuickActionPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(56.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Gray5
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
private fun LargeSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Gray5)
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Play button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = Gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
                    modifier = Modifier.size(56.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = Gray1
        )
    }
}

@Composable
private fun CompactSongRow(
    song: Song,
    onClick: () -> Unit,
    showPlayCount: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
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
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (showPlayCount && song.playCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Gray5
                ) {
                    Text(
                        text = "${song.playCount}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = AccentGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = AccentGreen,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(onNavigateToSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Gray5),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Gray1
            )
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = "Your Library is Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search for music to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray1
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNavigateToSearch,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen
            ),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Browse Music",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
}
