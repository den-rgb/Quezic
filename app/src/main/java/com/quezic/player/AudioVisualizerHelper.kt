package com.quezic.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.min

/**
 * Singleton helper that wraps Android's [Visualizer] API to capture
 * real-time FFT and waveform data from ExoPlayer's audio output.
 *
 * Exposes normalized data as [StateFlow]s for Compose consumption.
 *
 * Requires [Manifest.permission.RECORD_AUDIO] at runtime.
 */
@Singleton
class AudioVisualizerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioVisualizer"
        // Number of frequency bands to expose (downsampled from raw FFT)
        const val BAND_COUNT = 64
        // Max retries when Visualizer fails to start (common on Xiaomi/MIUI/HyperOS)
        private const val MAX_START_RETRIES = 3
        // Delay between retries (ms) – gives the audio pipeline time to settle
        private const val RETRY_DELAY_MS = 500L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var visualizer: Visualizer? = null
    private var currentAudioSessionId: Int = 0
    private var startRetryCount = 0

    // Normalized FFT magnitudes (0f..1f) with BAND_COUNT bands
    private val _fftData = MutableStateFlow(FloatArray(BAND_COUNT))
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    // Normalized waveform samples (-1f..1f)
    private val _waveformData = MutableStateFlow(FloatArray(0))
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    // Whether the visualizer is currently active
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    /**
     * Returns true if RECORD_AUDIO permission is granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start capturing audio data from the given audio session.
     * No-op if permission is not granted or session ID is invalid.
     *
     * On Xiaomi/MIUI/HyperOS devices, the Visualizer API often fails when
     * called too early (before the audio pipeline is fully initialized).
     * This method retries with a delay to handle that case, and falls back
     * to session ID 0 (mixed output capture) if the specific session fails.
     */
    fun start(audioSessionId: Int) {
        if (!hasPermission()) {
            Log.w(TAG, "RECORD_AUDIO permission not granted, visualizer disabled")
            return
        }

        if (audioSessionId == 0) {
            Log.w(TAG, "Invalid audio session ID (0), skipping visualizer start")
            return
        }

        // If already running on the same session, skip
        if (visualizer != null && currentAudioSessionId == audioSessionId && _isActive.value) {
            return
        }

        startRetryCount = 0
        startInternal(audioSessionId)
    }

    /**
     * Internal start with retry support for Xiaomi/HyperOS devices.
     * Falls back to session 0 (system-wide capture) if the specific session keeps failing.
     */
    private fun startInternal(audioSessionId: Int) {
        // Release any existing visualizer first
        releaseQuietly()
        currentAudioSessionId = audioSessionId

        try {
            visualizer = Visualizer(audioSessionId).apply {
                // Use maximum capture size for best resolution
                captureSize = Visualizer.getCaptureSizeRange()[1]

                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            vis: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { processWaveform(it) }
                        }

                        override fun onFftDataCapture(
                            vis: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { processFft(it) }
                        }
                    },
                    // Capture rate: ~60fps (Visualizer.getMaxCaptureRate() is typically 20000 mHz)
                    Visualizer.getMaxCaptureRate(),
                    true,  // waveform
                    true   // fft
                )

                enabled = true
            }

            _isActive.value = true
            startRetryCount = 0
            Log.d(TAG, "Visualizer started for session $audioSessionId, " +
                "captureSize=${visualizer?.captureSize}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start visualizer (attempt ${startRetryCount + 1}/$MAX_START_RETRIES) " +
                "for session $audioSessionId: ${e.message}")
            releaseQuietly()

            startRetryCount++
            if (startRetryCount < MAX_START_RETRIES) {
                // Retry after a delay - Xiaomi/HyperOS often needs time for the audio
                // pipeline to settle before the Visualizer can attach
                scope.launch {
                    delay(RETRY_DELAY_MS * startRetryCount)
                    startInternal(audioSessionId)
                }
            } else if (audioSessionId != 0) {
                // All retries exhausted for the specific session.
                // Fall back to session 0 which captures mixed audio output.
                // This works on most devices including Xiaomi, though it captures
                // all system audio rather than just our player.
                Log.w(TAG, "Falling back to session 0 (mixed output capture)")
                startRetryCount = 0
                startInternal(0)
            } else {
                Log.e(TAG, "Visualizer completely unavailable on this device")
            }
        }
    }

    /**
     * Release the visualizer without clearing state tracking.
     * Used internally during retry/restart sequences.
     */
    private fun releaseQuietly() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing visualizer: ${e.message}")
        }
        visualizer = null
    }

    /**
     * Stop capturing without releasing the visualizer (for pause).
     */
    fun stop() {
        try {
            visualizer?.enabled = false
            _isActive.value = false
            // Clear data to stop animations
            _fftData.value = FloatArray(BAND_COUNT)
            _waveformData.value = FloatArray(0)
            Log.d(TAG, "Visualizer stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping visualizer: ${e.message}")
        }
    }

    /**
     * Resume capturing after a stop.
     * If the visualizer was never successfully started (common on Xiaomi on first play),
     * this will attempt a full start using the provided or stored audio session ID.
     */
    fun resume(audioSessionId: Int = currentAudioSessionId) {
        if (!hasPermission()) return

        // If we have a new/different session ID, do a full restart
        if (audioSessionId != 0 && audioSessionId != currentAudioSessionId) {
            Log.d(TAG, "Audio session changed ($currentAudioSessionId -> $audioSessionId), restarting")
            start(audioSessionId)
            return
        }

        // If the visualizer was never created (failed init or first play), try full start
        if (visualizer == null) {
            val sessionToUse = if (audioSessionId != 0) audioSessionId else currentAudioSessionId
            if (sessionToUse != 0) {
                Log.d(TAG, "Visualizer not initialized, starting for session $sessionToUse")
                start(sessionToUse)
            }
            return
        }

        try {
            visualizer?.enabled = true
            _isActive.value = true
            Log.d(TAG, "Visualizer resumed")
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming visualizer: ${e.message}")
            // Full restart on resume failure
            if (currentAudioSessionId != 0) {
                start(currentAudioSessionId)
            }
        }
    }

    /**
     * Fully release the visualizer (for cleanup).
     */
    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing visualizer: ${e.message}")
        }
        visualizer = null
        _isActive.value = false
        _fftData.value = FloatArray(BAND_COUNT)
        _waveformData.value = FloatArray(0)
    }

    /**
     * Process raw FFT bytes into normalized magnitude bands.
     *
     * The FFT data from Visualizer is in the format:
     * [dc_real, dc_imag, f1_real, f1_imag, f2_real, f2_imag, ...]
     * We compute magnitude = sqrt(real^2 + imag^2) and downsample to BAND_COUNT bands.
     */
    private fun processFft(fft: ByteArray) {
        if (fft.size < 4) return

        // Number of frequency bins (half the FFT size, minus DC)
        val binCount = fft.size / 2 - 1
        if (binCount <= 0) return

        // Compute magnitudes for each frequency bin
        val magnitudes = FloatArray(binCount)
        var maxMagnitude = 1f

        for (i in 0 until binCount) {
            val real = fft[2 + i * 2].toFloat()
            val imag = fft[3 + i * 2].toFloat()
            val mag = hypot(real, imag)
            magnitudes[i] = mag
            if (mag > maxMagnitude) maxMagnitude = mag
        }

        // Downsample to BAND_COUNT bands using logarithmic distribution
        // (lower frequencies get fewer bins, higher get more - matches human perception)
        val bands = FloatArray(BAND_COUNT)
        for (band in 0 until BAND_COUNT) {
            // Map bands logarithmically to frequency bins
            val startBin = (binCount * Math.pow(band.toDouble() / BAND_COUNT, 2.0)).toInt()
            val endBin = min(
                binCount,
                (binCount * Math.pow((band + 1).toDouble() / BAND_COUNT, 2.0)).toInt() + 1
            )

            if (startBin < endBin) {
                var sum = 0f
                for (bin in startBin until endBin) {
                    sum += magnitudes[bin]
                }
                bands[band] = (sum / (endBin - startBin)) / maxMagnitude
            }
        }

        _fftData.value = bands
    }

    /**
     * Process raw waveform bytes into normalized float samples (-1f..1f).
     */
    private fun processWaveform(waveform: ByteArray) {
        val samples = FloatArray(waveform.size)
        for (i in waveform.indices) {
            // Unsigned byte (0..255) -> signed float (-1..1)
            samples[i] = (waveform[i].toInt() and 0xFF).toFloat() / 128f - 1f
        }
        _waveformData.value = samples
    }
}
