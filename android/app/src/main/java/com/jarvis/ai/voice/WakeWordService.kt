package com.jarvis.ai.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.ai.MainActivity
import com.jarvis.ai.R

/**
 * Foreground service for wake-word listening.
 * Uses Android-supported foreground service with visible notification.
 * Does NOT secretly record. Requires RECORD_AUDIO permission and user opt-in.
 */
class WakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_wake_channel"
        const val NOTIF_ID = 1001
        const val ACTION_START = "START_WAKE"
        const val ACTION_STOP = "STOP_WAKE"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            else -> {
                val notif = buildNotification()
                startForeground(NOTIF_ID, notif)
                // TODO: integrate on-device keyword spotting (e.g., using PocketSphinx or ML Kit)
                // For now, service stays alive with notification; actual wake detection delegated to SpeechRecognitionManager loop
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "JARVIS Wake Word", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Listens for wake word 'সবসময় প্রস্তুত' when enabled"
            }
            (getSystemService(NotificationManager::class.java))?.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS is listening")
            .setContentText("Wake word: সবসময় প্রস্তুত • Tap to open")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }
}

class VoiceForegroundService : Service() {
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Generic voice session foreground service
        return START_NOT_STICKY
    }
}
