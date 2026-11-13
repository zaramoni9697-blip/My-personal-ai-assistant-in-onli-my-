package com.jarvis.ai.device

import android.app.AlarmClock
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceActionResult(val success: Boolean, val message: String)

@Singleton
class DeviceController @Inject constructor(@ApplicationContext private val context: Context) {

    fun openApp(packageName: String): DeviceActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return DeviceActionResult(false, "App not found: $packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            DeviceActionResult(true, "Opened $packageName")
        } catch (e: Exception) { DeviceActionResult(false, e.message ?: "Failed to open app") }
    }

    fun openSettings(page: String = "general"): DeviceActionResult {
        return try {
            val intent = when(page.lowercase()) {
                "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                "battery" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            DeviceActionResult(true, "Opened settings: $page")
        } catch (e: Exception) { DeviceActionResult(false, e.message ?: "Cannot open settings") }
    }

    fun setAlarm(hour: Int, minute: Int, label: String = "JARVIS Alarm"): DeviceActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceActionResult(true, "Alarm set for ${hour}:${minute.toString().padStart(2,'0')}")
        } catch (e: Exception) { DeviceActionResult(false, e.message ?: "Alarm failed") }
    }

    fun setVolume(stream: Int = AudioManager.STREAM_MUSIC, level: Int, maxHint: Int? = null): DeviceActionResult {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(stream)
            val clamped = level.coerceIn(0, max)
            am.setStreamVolume(stream, clamped, 0)
            DeviceActionResult(true, "Volume set to $clamped / $max")
        } catch (e: Exception) { DeviceActionResult(false, e.message ?: "Volume control failed") }
    }

    fun toggleFlashlight(enable: Boolean): DeviceActionResult {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val camId = cm.cameraIdList.firstOrNull() ?: return DeviceActionResult(false, "No camera")
            cm.setTorchMode(camId, enable)
            DeviceActionResult(true, if (enable) "Flashlight ON" else "Flashlight OFF")
        } catch (e: Exception) { DeviceActionResult(false, "Flashlight not supported: ${e.message}") }
    }

    fun getBatteryInfo(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
        return "Battery: $level% ${if (isCharging) "(Charging)" else ""}"
    }

    fun getDeviceInfo(): String {
        return "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} | Android ${android.os.Build.VERSION.RELEASE} | SDK ${android.os.Build.VERSION.SDK_INT}"
    }

    fun startCall(phone: String): DeviceActionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$phone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceActionResult(true, "Opening dialer for $phone")
        } catch (e: Exception) { DeviceActionResult(false, e.message ?: "Call failed") }
    }

    fun sendSms(phone: String, body: String): DeviceActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:$phone")
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceActionResult(true, "Opening SMS composer")
        } catch (e: Exception) { DeviceActionResult(false, e.message ?: "SMS failed") }
    }

    fun getInstalledApps(): List<Pair<String,String>> {
        return try {
            context.packageManager.getInstalledApplications(0)
                .filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
                .map { it.packageName to (context.packageManager.getApplicationLabel(it).toString()) }
                .sortedBy { it.second }
                .take(100)
        } catch (_: Exception) { emptyList() }
    }
}
