package com.quezic

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.quezic.data.remote.NewPipeInit
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuezicApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize NewPipe Extractor
        NewPipeInit.init()
        
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Android notification channels are immutable after first creation.
            // Importance can only be lowered, never raised. Delete all old channels
            // so the new one is created fresh with the correct settings.
            LEGACY_PLAYBACK_CHANNEL_IDS.forEach { oldId ->
                notificationManager.deleteNotificationChannel(oldId)
            }

            // Playback notification channel
            // IMPORTANCE_DEFAULT is required for:
            // - Xiaomi HyperOS Music Island / Dynamic Island (camera pill)
            // - Lock screen media controls on MIUI/HyperOS/OneUI
            // - Status bar ongoing notification icon
            // Sound and vibration are disabled so it stays silent.
            val playbackChannel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(playbackChannel)

            // Download notification channel
            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                getString(R.string.notification_channel_download),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
            }
            notificationManager.createNotificationChannel(downloadChannel)
        }
    }

    companion object {
        // Versioned channel ID – bump when channel properties need to change.
        // Old IDs must be listed in LEGACY_PLAYBACK_CHANNEL_IDS for cleanup.
        const val PLAYBACK_CHANNEL_ID = "quezic_playback_v4"
        val LEGACY_PLAYBACK_CHANNEL_IDS = listOf(
            "quezic_playback", "quezic_playback_v2", "quezic_playback_v3"
        )
        const val DOWNLOAD_CHANNEL_ID = "quezic_download"
    }
}
