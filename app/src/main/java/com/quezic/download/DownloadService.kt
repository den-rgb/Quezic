package com.quezic.download

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service for managing downloads.
 * This service is kept alive while downloads are in progress.
 * 
 * Note: Most download logic is handled by WorkManager's DownloadWorker.
 * This service is primarily for maintaining foreground status and
 * showing persistent notification during downloads.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
