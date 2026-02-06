package com.quezic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.quezic.ui.theme.AccentGreen
import com.quezic.ui.theme.AccentPurple
import com.quezic.ui.theme.AccentPurpleDark
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-screen circular/radial visualizer for the Now Playing screen.
 * Draws frequency bands as lines radiating from a central circle,
 * creating a sun-burst effect behind the album art.
 */
@Composable
fun FullVisualizer(
    fftData: FloatArray,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    // Animate each band smoothly with spring-like decay
    val bandCount = min(fftData.size, 48) // Use 48 bands for the radial display
    val animatedBands = remember(bandCount) { arrayOfNulls<State<Float>>(bandCount) }

    for (i in 0 until bandCount) {
        val target = if (isPlaying && i < fftData.size) fftData[i].coerceIn(0f, 1f) else 0f
        animatedBands[i] = animateFloatAsState(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (target > (animatedBands[i]?.value ?: 0f)) 50 else 150,
                easing = LinearEasing
            ),
            label = "band_$i"
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val innerRadius = min(size.width, size.height) * 0.22f
        val maxBarLength = min(size.width, size.height) * 0.28f

        // Draw bands radiating outward in a full circle
        val totalBars = bandCount * 2 // Mirror for symmetry
        val angleStep = 360f / totalBars

        for (i in 0 until totalBars) {
            val bandIndex = if (i < bandCount) i else totalBars - 1 - i
            val magnitude = animatedBands[bandIndex.coerceIn(0, bandCount - 1)]?.value ?: 0f

            if (magnitude < 0.01f) continue

            val angleDeg = i * angleStep - 90f // Start from top
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            val barLength = maxBarLength * magnitude
            val startX = centerX + innerRadius * cosA
            val startY = centerY + innerRadius * sinA
            val endX = centerX + (innerRadius + barLength) * cosA
            val endY = centerY + (innerRadius + barLength) * sinA

            // Color gradient from green (low magnitude) to purple (high magnitude)
            val color = lerp(AccentGreen, AccentPurple, magnitude)
            val alpha = 0.4f + magnitude * 0.5f

            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw soft inner glow ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AccentGreen.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = innerRadius * 1.3f
            ),
            radius = innerRadius * 1.3f,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * Compact horizontal bar equalizer for the MiniPlayer.
 * Shows ~16 frequency bars that react to real FFT data.
 */
@Composable
fun MiniVisualizer(
    fftData: FloatArray,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val barCount = 16
    val animatedBars = remember(barCount) { arrayOfNulls<State<Float>>(barCount) }

    // Downsample FFT to barCount bands
    val step = if (fftData.isNotEmpty()) fftData.size.toFloat() / barCount else 1f

    for (i in 0 until barCount) {
        val fftIndex = (i * step).toInt().coerceIn(0, (fftData.size - 1).coerceAtLeast(0))
        val target = if (isPlaying && fftData.isNotEmpty()) fftData[fftIndex].coerceIn(0f, 1f) else 0f
        animatedBars[i] = animateFloatAsState(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (target > (animatedBars[i]?.value ?: 0f)) 40 else 120,
                easing = LinearEasing
            ),
            label = "minibar_$i"
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val totalWidth = size.width
        val barWidth = totalWidth / (barCount * 2f) // Space between bars
        val gap = barWidth * 0.4f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val magnitude = animatedBars[i]?.value ?: 0f
            if (magnitude < 0.02f) continue

            val barHeight = maxHeight * magnitude.coerceIn(0.05f, 1f)
            val x = i * (barWidth + gap)
            val yTop = (maxHeight - barHeight) / 2f
            val yBottom = yTop + barHeight

            val color = lerp(AccentGreen, AccentPurple, i.toFloat() / barCount)

            drawLine(
                color = color.copy(alpha = 0.7f + magnitude * 0.3f),
                start = Offset(x + barWidth / 2, yBottom),
                end = Offset(x + barWidth / 2, yTop),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Ambient background visualizer that creates a subtle pulsing glow
 * reacting to bass frequencies. Applied as an overlay on the main app scaffold.
 */
@Composable
fun AmbientVisualizer(
    fftData: FloatArray,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    // Extract bass intensity from first few frequency bands
    val bassIntensity by remember(fftData, isPlaying) {
        derivedStateOf {
            if (!isPlaying || fftData.size < 4) 0f
            else {
                // Average of first 4 bands (bass frequencies)
                val bass = (fftData[0] + fftData[1] + fftData[2] + fftData[3]) / 4f
                bass.coerceIn(0f, 1f)
            }
        }
    }

    val animatedBass by animateFloatAsState(
        targetValue = bassIntensity,
        animationSpec = tween(
            durationMillis = if (bassIntensity > 0.3f) 80 else 200,
            easing = LinearEasing
        ),
        label = "ambient_bass"
    )

    // Extract treble intensity for secondary glow
    val trebleIntensity by remember(fftData, isPlaying) {
        derivedStateOf {
            if (!isPlaying || fftData.size < 32) 0f
            else {
                val treble = (fftData[24] + fftData[28] + fftData[31]) / 3f
                treble.coerceIn(0f, 1f)
            }
        }
    }

    val animatedTreble by animateFloatAsState(
        targetValue = trebleIntensity,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "ambient_treble"
    )

    if (animatedBass < 0.01f && animatedTreble < 0.01f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val bottomY = size.height

        // Bass glow from bottom-center (green)
        val bassAlpha = (animatedBass * 0.12f).coerceIn(0f, 0.12f)
        if (bassAlpha > 0.005f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentGreen.copy(alpha = bassAlpha),
                        AccentGreen.copy(alpha = bassAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, bottomY),
                    radius = size.width * (0.5f + animatedBass * 0.3f)
                ),
                radius = size.width,
                center = Offset(centerX, bottomY)
            )
        }

        // Treble glow from top-center (purple)
        val trebleAlpha = (animatedTreble * 0.08f).coerceIn(0f, 0.08f)
        if (trebleAlpha > 0.005f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentPurpleDark.copy(alpha = trebleAlpha),
                        AccentPurple.copy(alpha = trebleAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, 0f),
                    radius = size.width * (0.4f + animatedTreble * 0.2f)
                ),
                radius = size.width,
                center = Offset(centerX, 0f)
            )
        }
    }
}

/**
 * Simple linear interpolation between two colors.
 */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}
