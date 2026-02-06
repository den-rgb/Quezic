package com.quezic.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.quezic.QuezicApp
import com.quezic.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background service for music playback using Media3/ExoPlayer.
 * Delegates to PlayerController's MediaSession for actual playback.
 * Provides:
 * - Background playback
 * - MediaSession for system integration
 * - Notification controls (play/pause/next/prev)
 * - Lock screen controls
 * - Xiaomi HyperOS Music Island (camera pill)
 * - Bluetooth/headphone button support
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val MEDIA_NOTIFICATION_ID = 2001
    }

    @Inject
    lateinit var playerController: PlayerController

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Ensure the notification channel exists.
        // Also created in QuezicApp.onCreate(), but repeated here in case the system
        // restarts the service independently.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Delete all legacy channels (Android channels are immutable once created)
            QuezicApp.LEGACY_PLAYBACK_CHANNEL_IDS.forEach { oldId ->
                manager.deleteNotificationChannel(oldId)
            }

            val channel = NotificationChannel(
                QuezicApp.PLAYBACK_CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)

            // Diagnostic: verify the channel was actually created with correct settings
            val created = manager.getNotificationChannel(QuezicApp.PLAYBACK_CHANNEL_ID)
            Log.d(TAG, "Channel '${QuezicApp.PLAYBACK_CHANNEL_ID}' created: " +
                "importance=${created?.importance}, " +
                "lockscreen=${created?.lockscreenVisibility}, " +
                "sound=${created?.sound}")
        }

        // Use Media3's DefaultMediaNotificationProvider with NO custom wrapping.
        // It already creates a proper MediaStyle notification with VISIBILITY_PUBLIC,
        // the session token, and playback actions. Custom wrappers can interfere
        // with how OEM skins (HyperOS, OneUI) detect media notifications.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(QuezicApp.PLAYBACK_CHANNEL_ID)
            .setChannelName(R.string.notification_channel_playback)
            .setNotificationId(MEDIA_NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(notificationProvider)

        // CRITICAL: Register the session with this service so it manages the notification.
        //
        // In the standard Media3 pattern, addSession() is called internally in onBind()
        // when a MediaController connects. But in this app, PlayerController manages the
        // MediaSession directly – no MediaController ever binds to this service.
        // Without addSession(), the internal MediaNotificationManager is never activated,
        // onUpdateNotification() is never called, and no notification is ever posted.
        playerController.mediaSession?.let { session ->
            addSession(session)
            Log.d(TAG, "Session added to service, notification management active")
        }

        Log.d(TAG, "PlaybackService created, channel=${QuezicApp.PLAYBACK_CHANNEL_ID}, " +
            "mediaSession=${playerController.mediaSession != null}, " +
            "player.mediaItemCount=${playerController.mediaSession?.player?.mediaItemCount}")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val session = playerController.mediaSession
        Log.d(TAG, "onGetSession: session=${session != null}, " +
            "player.playWhenReady=${session?.player?.playWhenReady}, " +
            "player.mediaItemCount=${session?.player?.mediaItemCount}")
        return session
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // Log notification updates for diagnostics
        val player = session.player
        Log.d(TAG, "onUpdateNotification: foregroundRequired=$startInForegroundRequired, " +
            "playWhenReady=${player.playWhenReady}, " +
            "mediaItemCount=${player.mediaItemCount}, " +
            "title=${player.currentMediaItem?.mediaMetadata?.title}")

        // Always request foreground to ensure the notification is posted via startForeground().
        // Without this, the system may only call notificationManager.notify() which some OEMs
        // (Xiaomi, Samsung) don't treat as a proper media notification.
        super.onUpdateNotification(session, true)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playerController.mediaSession?.player
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "PlaybackService destroyed")
        super.onDestroy()
    }
}

/**
 * Extension function to create a MediaItem from song data
 */
fun createMediaItem(
    id: String,
    title: String,
    artist: String,
    artworkUri: String?,
    mediaUri: String
): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(mediaUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
                .build()
        )
        .build()
}
