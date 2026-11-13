package com.mdfoysal.jarvis.feature.device

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional notification reader.
 *
 * Android only hands notifications to apps that the user explicitly enables in
 * *Settings → Notifications → Device & app notifications*. Without that grant JARVIS reports
 * "notification access is not granted" instead of pretending it can read them.
 */
class JarvisNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        NotificationStore.add(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        NotificationStore.remove(sbn.key)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        runCatching {
            activeNotifications?.forEach { NotificationStore.add(it) }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationStore.clear()
    }

    companion object {
        /** True when the user granted notification access to JARVIS. */
        fun isEnabled(): Boolean = NotificationStore.connected
    }
}

/** In-memory cache of the notifications the system shared with JARVIS. */
object NotificationStore {

    private val items = LinkedHashMap<String, StatusBarNotification>()

    @Volatile
    var connected: Boolean = false
        internal set

    fun add(sbn: StatusBarNotification) {
        synchronized(items) { items[sbn.key] = sbn }
        connected = true
    }

    fun remove(key: String) {
        synchronized(items) { items.remove(key) }
    }

    fun clear() {
        synchronized(items) { items.clear() }
        connected = false
    }

    /** Snapshot of the currently visible notifications, newest first. */
    fun snapshot(): List<StatusBarNotification> = synchronized(items) {
        items.values.sortedByDescending { it.postTime }
    }
}
