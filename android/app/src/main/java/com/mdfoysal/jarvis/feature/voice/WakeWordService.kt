package com.mdfoysal.jarvis.feature.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mdfoysal.jarvis.MainActivity
import com.mdfoysal.jarvis.R
import java.util.Locale

/**
 * Wake-word listener.
 *
 * Android requires background microphone access to go through a **foreground service with a
 * visible notification** — that is exactly what this service does. The user can always see
 * (and stop) the listening notification, and Android shows its own microphone indicator.
 *
 * Wake phrase: "সবসময় প্রস্তুত" (configurable in Settings → Voice).
 */
class WakeWordService : Service() {

    private var controller: SpeechRecognizerController? = null
    private var wakePhrase: String = DEFAULT_WAKE_WORD
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        controller = SpeechRecognizerController(this, object : SpeechRecognizerController.Listener {
            override fun onReady() = Unit

            override fun onPartialResult(text: String) {
                if (matchesWakePhrase(text)) triggerWake()
            }

            override fun onFinalResult(text: String) {
                if (matchesWakePhrase(text)) triggerWake()
                if (running) restartListening()
            }

            override fun onError(message: String) {
                if (running) restartListening()
            }

            override fun onEndOfSpeech() = Unit

            override fun onRmsChanged(rms: Float) = Unit
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakePhrase = intent?.getStringExtra(EXTRA_WAKE_PHRASE)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_WAKE_WORD
        running = true
        startForegroundCompat()
        restartListening()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        controller?.cancel()
        controller?.destroy()
        controller = null
        super.onDestroy()
    }

    private fun restartListening() {
        controller?.start(com.mdfoysal.jarvis.core.text.AssistantLanguage.BENGALI)
    }

    private fun matchesWakePhrase(text: String): Boolean {
        if (text.isBlank()) return false
        val normalized = text.trim().lowercase(Locale.US)
            .replace(Regex("[\\s\\p{Punct}]+"), "")
        val target = wakePhrase.trim().lowercase(Locale.US)
            .replace(Regex("[\\s\\p{Punct}]+"), "")
        return target.isNotEmpty() && normalized.contains(target)
    }

    private fun triggerWake() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_WAKE_TRIGGERED, true)
        }
        startActivity(intent)
        stopSelf()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_text))
            .setSmallIcon(R.drawable.ic_stat_jarvis)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.wake_word_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.wake_word_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "jarvis_wake_word"
        const val NOTIFICATION_ID = 4201
        const val EXTRA_WAKE_PHRASE = "extra_wake_phrase"
        const val EXTRA_WAKE_TRIGGERED = "extra_wake_triggered"
        const val DEFAULT_WAKE_WORD = "সবসময় প্রস্তুত"

        fun start(context: android.content.Context, wakePhrase: String) {
            val intent = Intent(context, WakeWordService::class.java)
                .putExtra(EXTRA_WAKE_PHRASE, wakePhrase)
            context.startForegroundService(intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }
}
