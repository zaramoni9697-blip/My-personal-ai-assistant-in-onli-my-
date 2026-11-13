package com.mdfoysal.jarvis.core.persona

import com.mdfoysal.jarvis.core.text.AssistantLanguage
import com.mdfoysal.jarvis.core.text.LanguageDetector

/** Phone actions JARVIS is allowed to request (subject to Android permission + confirmation). */
enum class DeviceAction {
    OPEN_APP,
    SET_ALARM,
    CREATE_REMINDER,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    BATTERY_INFO,
    DEVICE_INFO,
    OPEN_SETTINGS,
    READ_NOTIFICATIONS,
    START_CALL,
    SEND_SMS,
    OPEN_WEB,
    UNKNOWN
}

/** A parsed, still-unapproved device command. */
data class DeviceCommand(
    val action: DeviceAction,
    val target: String = "",
    val time: String = "",
    val requiresConfirmation: Boolean = true,
    val confidence: Float = 0f,
    val sourceText: String = ""
) {
    val isKnown: Boolean get() = action != DeviceAction.UNKNOWN
}

/**
 * Turns a natural-language sentence into a [DeviceCommand].
 *
 * Pure Kotlin on purpose: the parsing rules are unit-tested on the JVM and the Android layer
 * only has to translate the resulting command into intents.
 */
object DeviceIntentParser {

    private val openAppEn = listOf("open app ", "launch ", "open ", "start ")
    private val openAppBn = listOf("অ্যাপ খোলো", "অ্যাপ খুলো", "অ্যাপ চালু করো", "খুলে দাও", "ওপেন করো", "চালু করো")

    private val alarmEn = listOf("set an alarm", "set alarm", "alarm at", "wake me at", "alarm for")
    private val alarmBn = listOf("অ্যালার্ম", "আলার্ম", "ঘুম ভাঙাও", "ঘুম ভাঙিয়ে")

    private val reminderEn = listOf("remind me to", "reminder", "set a reminder", "don't let me forget")
    private val reminderBn = listOf("মনে করিয়ে দাও", "মনে করাও", "রিমাইন্ডার", "মনে রাখা")

    private val volumeUpEn = listOf("volume up", "increase volume", "turn up the volume", "louder", "raise volume")
    private val volumeUpBn = listOf("ভলিউম বাড়াও", "ভলিউম বাড়ান", "শব্দ বাড়াও", "আওয়াজ বাড়াও")

    private val volumeDownEn = listOf("volume down", "decrease volume", "turn down the volume", "quieter", "lower volume")
    private val volumeDownBn = listOf("ভলিউম কমাও", "ভলিউম কমান", "শব্দ কমাও", "আওয়াজ কমাও")

    private val muteEn = listOf("mute", "silence the phone", "mute the volume")
    private val muteBn = listOf("মিউট", "চুপ করাও", "নীরব করো")

    private val flashlightOnEn = listOf(
        "flashlight on", "turn on flashlight", "turn on the flashlight",
        "turn on the torch", "torch on", "enable flashlight", "switch on flashlight"
    )
    private val flashlightOnBn = listOf("ফ্ল্যাশলাইট জ্বালাও", "ফ্ল্যাশলাইট অন", "টর্চ জ্বালাও", "টর্চ অন")

    private val flashlightOffEn = listOf(
        "flashlight off", "turn off flashlight", "turn off the flashlight",
        "turn off the torch", "torch off", "disable flashlight"
    )
    private val flashlightOffBn = listOf("ফ্ল্যাশলাইট বন্ধ", "ফ্ল্যাশলাইট অফ", "টর্চ বন্ধ", "টর্চ অফ")

    private val batteryEn = listOf("battery", "battery status", "battery level", "how much battery")
    private val batteryBn = listOf("ব্যাটারি", "ব্যাটারির চার্জ", "ব্যাটারি স্টেটাস")

    private val deviceInfoEn = listOf("device info", "phone info", "device information", "system information")
    private val deviceInfoBn = listOf("ডিভাইস তথ্য", "ফোনের তথ্য", "ডিভাইস ইনফো")

    private val settingsEn = listOf("open settings", "device settings", "system settings", "app settings")
    private val settingsBn = listOf("সেটিংস খোলো", "সেটিংস খুলো", "সেটিংস")

    private val notificationsEn = listOf("read notifications", "notification", "my notifications", "check notifications")
    private val notificationsBn = listOf("নোটিফিকেশন", "নোটিফিকেশন পড়ো", "পরীক্ষা করো নোটিফিকেশন")

    private val callEn = listOf("call ", "phone ", "dial ", "ring ")
    private val callBn = listOf("ফোন করো", "কল করো", "ফোন কর")

    private val smsEn = listOf("send a message to", "send message to", "text ", "sms to", "message ")
    private val smsBn = listOf("মেসেজ করো", "মেসেজ পাঠাও", "এসএমএস", "বার্তা পাঠাও")

    private val webEn = listOf("open website", "open browser", "search google", "go to ")
    private val webBn = listOf("ওয়েবসাইট খোলো", "ব্রাউজার খোলো", "গুগলে সার্চ")

    /** Parses [rawText] into a device command (or [DeviceAction.UNKNOWN]). */
    fun parse(rawText: String): DeviceCommand {
        val text = rawText.lowercase().trim()
        if (text.isEmpty()) return DeviceCommand(DeviceAction.UNKNOWN, sourceText = rawText)
        val bengali = LanguageDetector.detect(rawText) == AssistantLanguage.BENGALI

        fun match(en: List<String>, bn: List<String>, action: DeviceAction, target: String = "", time: String = ""): DeviceCommand? {
            val hit = en.firstOrNull { text.contains(it) } ?: bn.firstOrNull { text.contains(it) }
            if (hit == null) return null
            return DeviceCommand(
                action = action,
                target = target,
                time = time,
                requiresConfirmation = actionNeedsConfirmation(action),
                confidence = 0.9f,
                sourceText = rawText
            )
        }

        // Specific first, general later (e.g. "flashlight on" before "open ...").
        match(flashlightOnEn, flashlightOnBn, DeviceAction.FLASHLIGHT_ON)?.let { return it }
        match(flashlightOffEn, flashlightOffBn, DeviceAction.FLASHLIGHT_OFF)?.let { return it }
        match(volumeUpEn, volumeUpBn, DeviceAction.VOLUME_UP)?.let { return it }
        match(volumeDownEn, volumeDownBn, DeviceAction.VOLUME_DOWN)?.let { return it }
        match(muteEn, muteBn, DeviceAction.VOLUME_MUTE)?.let { return it }
        match(batteryEn, batteryBn, DeviceAction.BATTERY_INFO)?.let { return it }
        match(deviceInfoEn, deviceInfoBn, DeviceAction.DEVICE_INFO)?.let { return it }
        match(notificationsEn, notificationsBn, DeviceAction.READ_NOTIFICATIONS)?.let { return it }
        match(reminderEn, reminderBn, DeviceAction.CREATE_REMINDER)?.let {
            return it.copy(target = extractReminderTarget(text))
        }
        match(alarmEn, alarmBn, DeviceAction.SET_ALARM)?.let {
            return it.copy(time = extractTime(text), target = extractAlarmLabel(text))
        }
        match(smsEn, smsBn, DeviceAction.SEND_SMS)?.let {
            return it.copy(target = extractAfterKeyword(text, smsEn + smsBn))
        }
        match(callEn, callBn, DeviceAction.START_CALL)?.let {
            return it.copy(target = extractAfterKeyword(text, callEn + callBn))
        }
        match(settingsEn, settingsBn, DeviceAction.OPEN_SETTINGS)?.let { return it }
        match(webEn, webBn, DeviceAction.OPEN_WEB)?.let {
            return it.copy(target = extractAfterKeyword(text, webEn + webBn))
        }
        match(openAppEn, openAppBn, DeviceAction.OPEN_APP)?.let {
            return it.copy(target = extractAfterKeyword(text, openAppEn + openAppBn))
        }

        return DeviceCommand(DeviceAction.UNKNOWN, sourceText = rawText)
    }

    /** Read-only actions do not need a confirmation dialog. */
    fun actionNeedsConfirmation(action: DeviceAction): Boolean = when (action) {
        DeviceAction.BATTERY_INFO,
        DeviceAction.DEVICE_INFO,
        DeviceAction.OPEN_SETTINGS,
        DeviceAction.UNKNOWN -> false
        else -> true
    }

    private fun extractAfterKeyword(text: String, keywords: List<String>): String {
        for (keyword in keywords.sortedByDescending { it.length }) {
            val index = text.indexOf(keyword)
            if (index >= 0) {
                val rest = text.substring(index + keyword.length).trim()
                    .trim('.', ',', '!', '?', '"', '\'')
                if (rest.isNotEmpty()) return rest
            }
        }
        return ""
    }

    private fun extractReminderTarget(text: String): String {
        val en = extractAfterKeyword(text, listOf("remind me to", "set a reminder to", "reminder to", "reminder for"))
        if (en.isNotEmpty()) return en
        val bn = extractAfterKeyword(text, listOf("মনে করিয়ে দাও", "মনে করাও"))
        if (bn.isNotEmpty()) return bn
        return text
    }

    private fun extractAlarmLabel(text: String): String {
        val stripped = text.replace(Regex("(set|an|the)?\\s*(alarm|অ্যালার্ম|আলার্ম)"), "")
            .replace(Regex("\\b(at|for|on|এ|থেকে)?\\s*\\d{1,2}(:\\d{2})?\\s*(am|pm)?\\b"), "")
            .trim()
            .trim('.', ',')
        return stripped.ifBlank { "Alarm" }
    }

    /** Extracts "7", "07:30", "7 am", "19:00" style times from a sentence. */
    fun extractTime(text: String): String {
        val pattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
        for (match in pattern.findAll(text.lowercase())) {
            val hour = match.groupValues[1].toIntOrNull() ?: continue
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val meridiem = match.groupValues[3]
            if (hour > 23 || minute > 59) continue
            val normalized = when {
                meridiem == "pm" && hour < 12 -> (hour + 12).toString().padStart(2, '0')
                meridiem == "am" && hour == 12 -> "00"
                else -> hour.toString().padStart(2, '0')
            }
            return "$normalized:${minute.toString().padStart(2, '0')}"
        }
        return ""
    }
}
