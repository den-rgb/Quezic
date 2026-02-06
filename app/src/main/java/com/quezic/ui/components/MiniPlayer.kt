package com.quezic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quezic.domain.model.PlayerState
import com.quezic.ui.theme.*

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    onOpenInYouTube: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Gray6,
        tonalElevation = 0.dp
    ) {
        Column {
            // Progress bar at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Gray4)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(playerState.progress)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AccentGreen, SystemPurple)
                            )
                        )
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Gray5),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Playing indicator
                    if (playerState.isPlaying) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(AccentGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayingBars()
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                // Song info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
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
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Show YouTube button if playback failed with authentication error
                if (playerState.canOpenInYouTube) {
                    Surface(
                        onClick = onOpenInYouTube,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFF0000) // YouTube red
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Open in YouTube",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YouTube",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Play/Pause button
                    Surface(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        color = AccentGreen
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Next button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayingBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "playing")
    
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    
    Row(
        modifier = Modifier.size(8.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight(bar1)
                .background(Color.White, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight(bar2)
                .background(Color.White, RoundedCornerShape(1.dp))
        )
    }
}
