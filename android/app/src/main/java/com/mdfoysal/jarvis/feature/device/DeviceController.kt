package com.mdfoysal.jarvis.feature.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mdfoysal.jarvis.core.persona.DeviceAction
import com.mdfoysal.jarvis.core.persona.DeviceCommand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Outcome of a device action. */
data class DeviceResult(
    val success: Boolean,
    val message: String,
    val needsPermission: String? = null
)

/** An installed application that JARVIS can launch. */
data class InstalledApp(
    val label: String,
    val packageName: String
)

/**
 * Executes the phone actions JARVIS understands.
 *
 * Everything here uses public Android APIs and user-approved intents:
 * nothing bypasses Android security, and actions that change state are only executed after the
 * user confirmed them in the UI.
 */
class DeviceController(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** Executes a parsed command. */
    fun execute(command: DeviceCommand): DeviceResult {
        if (!command.isKnown) {
            return DeviceResult(false, "I do not know how to do that on this phone yet")
        }
        return when (command.action) {
            DeviceAction.OPEN_APP -> openApp(command.target)
            DeviceAction.SET_ALARM -> setAlarm(command.time, command.target)
            DeviceAction.CREATE_REMINDER -> createReminder(command.target)
            DeviceAction.VOLUME_UP -> adjustVolume(AudioManager.FLAG_SHOW_UI, true)
            DeviceAction.VOLUME_DOWN -> adjustVolume(AudioManager.FLAG_SHOW_UI, false)
            DeviceAction.VOLUME_MUTE -> muteVolume()
            DeviceAction.FLASHLIGHT_ON -> setFlashlight(true)
            DeviceAction.FLASHLIGHT_OFF -> setFlashlight(false)
            DeviceAction.BATTERY_INFO -> batteryInfo()
            DeviceAction.DEVICE_INFO -> deviceInfo()
            DeviceAction.OPEN_SETTINGS -> openSettings()
            DeviceAction.READ_NOTIFICATIONS -> readNotifications()
            DeviceAction.START_CALL -> startCall(command.target)
            DeviceAction.SEND_SMS -> sendSms(command.target)
            DeviceAction.OPEN_WEB -> openWeb(command.target)
            DeviceAction.UNKNOWN -> DeviceResult(false, "Unknown action")
        }
    }

    // ------------------------------------------------------------------ actions

    /** Opens an installed app by its display name. */
    fun openApp(label: String): DeviceResult {
        val app = findApp(label)
            ?: return DeviceResult(false, "I could not find an app called '$label'")
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            ?: return DeviceResult(false, "'${app.label}' cannot be opened right now")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launch(intent, "Opening ${app.label}")
    }

    /** Creates an alarm through the system clock app. */
    fun setAlarm(time: String, label: String): DeviceResult {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
            ?: return DeviceResult(false, "I need a time like 07:30 to set an alarm")
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (label.isNotBlank() && label != "Alarm") {
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
            }
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return launch(intent, "Setting an alarm for ${String.format(Locale.US, "%02d:%02d", hour, minute)}")
    }

    /** Opens the clock app's reminder screen. */
    fun createReminder(text: String): DeviceResult {
        val intent = Intent(AlarmClock.ACTION_CREATE_REMINDER).apply {
            if (text.isNotBlank()) {
                putExtra(AlarmClock.EXTRA_REMINDER_TEXT, text)
            }
        }
        return launch(intent, "Creating a reminder")
    }

    /** Raises or lowers the media volume. */
    fun adjustVolume(up: Boolean): DeviceResult {
        val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return DeviceResult(true, if (up) "Volume raised" else "Volume lowered")
    }

    fun muteVolume(): DeviceResult {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI
        )
        return DeviceResult(true, "Volume muted")
    }

    /** Toggles the camera torch (available on devices with a flash). */
    fun setFlashlight(on: Boolean): DeviceResult {
        if (!hasPermission(Manifest.permission.CAMERA)) {
            return DeviceResult(
                success = false,
                message = "Camera permission is required to control the flashlight",
                needsPermission = Manifest.permission.CAMERA
            )
        }
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return DeviceResult(false, "This device has no camera service")
        return try {
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return DeviceResult(false, "This device has no flashlight")
            manager.setTorchMode(cameraId, on)
            DeviceResult(true, if (on) "Flashlight on" else "Flashlight off")
        } catch (e: Exception) {
            DeviceResult(false, e.message ?: "Could not control the flashlight")
        }
    }

    /** Reads the battery level through the sticky battery broadcast. */
    fun batteryInfo(): DeviceResult {
        val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val temperature = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val chargingText = if (charging) " (charging)" else ""
        val pluggedText = if (plugged == 0) " on battery" else ""
        val temperatureText = String.format(Locale.US, "%.1f", temperature)
        return DeviceResult(
            success = true,
            message = "Battery: $percent%$chargingText$pluggedText, temperature $temperatureText°C"
        )
    }

    /** Device and software information. */
    fun deviceInfo(): DeviceResult {
        val metrics = context.resources.displayMetrics
        val info = buildString {
            append("Manufacturer: ${Build.MANUFACTURER}\n")
            append("Model: ${Build.MODEL}\n")
            append("Device: ${Build.DEVICE}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("Screen: ${metrics.widthPixels}×${metrics.heightPixels} @ ${metrics.densityDpi}dpi\n")
            append("Locale: ${Locale.getDefault().displayName}\n")
            append("Build: ${Build.ID}")
        }
        return DeviceResult(true, info)
    }

    /** Opens the system settings (optionally a specific page). */
    fun openSettings(page: SettingsPage = SettingsPage.ROOT): DeviceResult {
        val intent = when (page) {
            SettingsPage.ROOT -> Intent(Settings.ACTION_SETTINGS)
            SettingsPage.WIRELESS -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            SettingsPage.BATTERY -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            SettingsPage.APPLICATIONS -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            SettingsPage.LANGUAGE -> Intent(Settings.ACTION_LOCALE_SETTINGS)
            SettingsPage.NOTIFICATION -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            SettingsPage.VOICE_INPUT -> Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        }
        return launch(intent, "Opening ${page.displayName}")
    }

    /** Reads the notifications the system shared with JARVIS. */
    fun readNotifications(): DeviceResult {
        if (!JarvisNotificationListener.isEnabled()) {
            return DeviceResult(
                success = false,
                message = "Notification access is not granted. Enable it in Settings → Notifications → Device & app notifications.",
                needsPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            )
        }
        val notifications = NotificationStore.snapshot()
        if (notifications.isEmpty()) {
            return DeviceResult(true, "There are no active notifications right now")
        }
        val summary = notifications.take(5).joinToString("\n") { sbn ->
            val title = sbn.notification?.extras?.getCharSequence("android.title")?.toString().orEmpty()
            val text = sbn.notification?.extras?.getCharSequence("android.text")?.toString().orEmpty()
            "- ${title.ifBlank { sbn.packageName }}: ${text.take(80)}"
        }
        return DeviceResult(true, "${notifications.size} active notification(s):\n$summary")
    }

    /** Opens the dialler for the given number (the user confirms the call). */
    fun startCall(target: String): DeviceResult {
        val number = target.trim()
        if (number.isEmpty()) return DeviceResult(false, "I need a phone number to call")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        return launch(intent, "Opening the dialler for $number")
    }

    /** Opens the SMS composer for the given contact (the user sends the message). */
    fun sendSms(target: String, body: String = ""): DeviceResult {
        val recipient = target.trim()
        if (recipient.isEmpty()) return DeviceResult(false, "I need a contact to send a message to")
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipient")).apply {
            if (body.isNotBlank()) putExtra("sms_body", body)
        }
        return launch(intent, "Opening the message composer for $recipient")
    }

    /** Opens a website or performs a search. */
    fun openWeb(target: String): DeviceResult {
        val value = target.trim()
        val url = when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.contains('.') && !value.contains(' ') -> "https://$value"
            else -> "https://www.google.com/search?q=${Uri.encode(value)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return launch(intent, "Opening $url")
    }

    // ------------------------------------------------------------------ helpers

    /** Lists launchable apps so the UI can show what JARVIS can open. */
    fun installedApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = context.packageManager.queryIntentActivities(intent, 0)
        return packages
            .map { info ->
                InstalledApp(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = info.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Finds an installed app whose label contains [label]. */
    fun findApp(label: String): InstalledApp? {
        val needle = label.trim().lowercase()
        if (needle.isEmpty()) return null
        return installedApps().firstOrNull { app ->
            app.label.lowercase() == needle || app.label.lowercase().contains(needle)
        }
    }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun launch(intent: Intent, successMessage: String): DeviceResult = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        DeviceResult(true, successMessage)
    } catch (e: Exception) {
        DeviceResult(false, e.message ?: "This action is not supported on this device")
    }

    /** The settings pages JARVIS can open. */
    enum class SettingsPage(val displayName: String) {
        ROOT("Settings"),
        WIRELESS("Wireless & network settings"),
        BATTERY("Battery settings"),
        APPLICATIONS("App settings"),
        LANGUAGE("Language & input settings"),
        NOTIFICATION("Notification access settings"),
        VOICE_INPUT("Voice input settings")
    }

    companion object {
        /** Permissions JARVIS may ask for, with a human readable explanation. */
        fun permissionExplanations(): List<Pair<String, String>> = listOf(
            Manifest.permission.RECORD_AUDIO to
                "Microphone — used only while you are listening or when the wake word service is running.",
            Manifest.permission.POST_NOTIFICATIONS to
                "Notifications — shows the visible 'JARVIS is listening' notification.",
            Manifest.permission.CAMERA to
                "Camera — required by Android to switch the flashlight on and off.",
            Manifest.permission.CALL_PHONE to
                "Phone — allows JARVIS to place a call directly instead of opening the dialler.",
            Manifest.permission.SEND_SMS to
                "Messages — allows JARVIS to send a message directly instead of opening the composer."
        )
    }
}
