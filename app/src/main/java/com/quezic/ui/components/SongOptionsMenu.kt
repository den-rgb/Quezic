package com.quezic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quezic.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: (() -> Unit)?,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
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
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Options
            OptionItem(
                icon = Icons.Default.PlayArrow,
                text = "Play",
                onClick = {
                    onPlay()
                    onDismiss()
                }
            )
            
            OptionItem(
                icon = Icons.Default.QueueMusic,
                text = "Add to queue",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )
            
            OptionItem(
                icon = Icons.Default.PlaylistAdd,
                text = "Add to playlist",
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }
            )
            
            OptionItem(
                icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                text = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            )
            
            if (onDownload != null && !song.isDownloaded) {
                OptionItem(
                    icon = Icons.Default.Download,
                    text = "Download",
                    onClick = {
                        onDownload()
                        onDismiss()
                    }
                )
            }
            
            OptionItem(
                icon = Icons.Default.Share,
                text = "Share",
                onClick = {
                    onShare()
                    onDismiss()
                }
            )
            
            if (onDelete != null) {
                OptionItem(
                    icon = Icons.Default.Delete,
                    text = "Delete from library",
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    isDestructive = true
                )
            }
        }
    }
}

@Composable
private fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
