package verify

import com.mdfoysal.jarvis.core.crypto.PasswordHasher
import com.mdfoysal.jarvis.core.crypto.SecretBox
import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.persona.DeviceAction
import com.mdfoysal.jarvis.core.persona.DeviceIntentParser
import com.mdfoysal.jarvis.core.persona.JarvisPersona
import com.mdfoysal.jarvis.core.providers.ProviderRegistry
import com.mdfoysal.jarvis.core.text.AssistantLanguage
import com.mdfoysal.jarvis.core.text.LanguageDetector
import java.io.File

object CoreChecks {

    fun run(providersJsonPath: String) {
        jsonChecks()
        languageChecks()
        personaChecks()
        deviceIntentChecks()
        cryptoChecks()
        registryChecks(providersJsonPath)
    }

    private fun jsonChecks() {
        TestKit.suite("JSON engine")
        val parsed = Json.parse("""{"a":1,"b":[true,null,"x"],"c":{"d":2.5},"e":"héllo\n"}""")
        TestKit.checkEquals(1, parsed.optInt("a"), "reads integer field")
        TestKit.checkEquals(3, parsed.optArray("b").size, "reads array field")
        TestKit.checkEquals(2.5, parsed.optDouble("c.d"), "reads nested double")
        TestKit.checkEquals("héllo\n", parsed.optString("e"), "reads escaped string")
        TestKit.checkEquals(2, parsed.path("c.d")?.asIntOrNull(), "path lookup for nested numeric")
        TestKit.check(parsed.path("missing.path") == null, "path lookup returns null when missing")

        val encoded = Json.encode(parsed)
        val reparsed = Json.parse(encoded)
        TestKit.checkEquals("héllo\n", reparsed.optString("e"), "round-trip preserves escapes")

        val built = Json.parse("{\"k\":\"va\\\"lue\"}")
        TestKit.checkEquals("va\"lue", built.optString("k"), "escaped quote parsing")

        TestKit.check(Json.parseOrNull("not json") == null, "invalid JSON returns null instead of throwing")
        TestKit.check(Json.parseOrNull(null) == null, "null input returns null")

        val nested = Json.parse("""{"choices":[{"message":{"content":"hello"}}]}""")
        TestKit.checkEquals("hello", nested.path("choices.0.message.content")?.asStringOrNull(), "array path lookup")
    }

    private fun languageChecks() {
        TestKit.suite("Language detection")
        TestKit.checkEquals(
            AssistantLanguage.BENGALI,
            LanguageDetector.detect("তুমি কে?"),
            "detects Bengali"
        )
        TestKit.checkEquals(
            AssistantLanguage.ENGLISH,
            LanguageDetector.detect("Who are you?"),
            "detects English"
        )
        TestKit.checkEquals(
            AssistantLanguage.MIXED,
            LanguageDetector.detect("আজকে weather কেমন?"),
            "detects mixed Bengali + English"
        )
        TestKit.checkEquals(
            AssistantLanguage.ENGLISH,
            LanguageDetector.detect(""),
            "empty text falls back to English"
        )
    }

    private fun personaChecks() {
        TestKit.suite("JARVIS persona / identity")
        val en = JarvisPersona.localReply("Who created you?")
        TestKit.check(en != null && en.text.contains("Md Foysal"), "English creator question answered with Md Foysal")
        TestKit.check(en != null && en.text.contains("I was created by Md Foysal"), "English creator answer wording")

        val bn = JarvisPersona.localReply("তোমাকে কে তৈরি করেছে?")
        TestKit.check(bn != null && bn.text.contains("Md Foysal"), "Bengali creator question answered with Md Foysal")
        TestKit.check(bn != null && bn.language == AssistantLanguage.BENGALI, "Bengali creator answer detected as Bengali")

        val identity = JarvisPersona.localReply("Who are you?")
        TestKit.check(identity != null && identity.text.contains("JARVIS AI"), "identity question mentions JARVIS AI")

        val greeting = JarvisPersona.localReply("Hello Jarvis")
        TestKit.check(greeting != null && greeting.text.contains("What can I help you with"), "greeting answered locally")

        TestKit.check(JarvisPersona.localReply("Write me a poem about the sea") == null, "unrelated question is not handled locally")

        val prompt = JarvisPersona.systemPrompt(AssistantLanguage.BENGALI, listOf("User prefers Bangla"))
        TestKit.check(prompt.contains("Md Foysal"), "system prompt states the creator")
        TestKit.check(prompt.contains("User prefers Bangla"), "system prompt injects memory")
    }

    private fun deviceIntentChecks() {
        TestKit.suite("Device intent parsing")
        val open = DeviceIntentParser.parse("Open WhatsApp")
        TestKit.checkEquals(DeviceAction.OPEN_APP, open.action, "detects 'open app'")
        TestKit.checkEquals("whatsapp", open.target, "extracts app name")

        val alarm = DeviceIntentParser.parse("set an alarm at 7:30")
        TestKit.checkEquals(DeviceAction.SET_ALARM, alarm.action, "detects alarm request")
        TestKit.checkEquals("07:30", alarm.time, "extracts alarm time")

        val alarmPm = DeviceIntentParser.parse("set alarm at 7 pm")
        TestKit.checkEquals("19:00", alarmPm.time, "normalises pm times to 24h")

        val reminder = DeviceIntentParser.parse("remind me to call mom at 8")
        TestKit.checkEquals(DeviceAction.CREATE_REMINDER, reminder.action, "detects reminder request")
        TestKit.check(reminder.target.contains("call mom"), "extracts reminder content")

        TestKit.checkEquals(DeviceAction.VOLUME_UP, DeviceIntentParser.parse("volume up").action, "detects volume up")
        TestKit.checkEquals(DeviceAction.VOLUME_DOWN, DeviceIntentParser.parse("ভলিউম কমাও").action, "detects Bengali volume down")
        TestKit.checkEquals(DeviceAction.FLASHLIGHT_ON, DeviceIntentParser.parse("turn on the flashlight").action, "detects flashlight on")
        TestKit.checkEquals(DeviceAction.FLASHLIGHT_OFF, DeviceIntentParser.parse("flashlight off").action, "detects flashlight off")
        TestKit.checkEquals(DeviceAction.BATTERY_INFO, DeviceIntentParser.parse("battery status").action, "detects battery query")
        TestKit.checkEquals(DeviceAction.DEVICE_INFO, DeviceIntentParser.parse("device info").action, "detects device info query")
        TestKit.checkEquals(DeviceAction.OPEN_SETTINGS, DeviceIntentParser.parse("open settings").action, "detects settings request")
        TestKit.checkEquals(DeviceAction.READ_NOTIFICATIONS, DeviceIntentParser.parse("read notifications").action, "detects notification request")
        TestKit.checkEquals(DeviceAction.START_CALL, DeviceIntentParser.parse("call 01712345678").action, "detects call request")
        TestKit.checkEquals(DeviceAction.SEND_SMS, DeviceIntentParser.parse("send a message to Rahim").action, "detects sms request")
        TestKit.checkEquals("rahim", DeviceIntentParser.parse("send a message to Rahim").target, "extracts sms recipient")

        TestKit.check(!DeviceIntentParser.parse("what is the weather today").isKnown, "ordinary question is not a device command")
        TestKit.check(DeviceIntentParser.parse("flashlight on").requiresConfirmation, "state-changing action requires confirmation")
        TestKit.check(!DeviceIntentParser.parse("battery status").requiresConfirmation, "read-only action needs no confirmation")
    }

    private fun cryptoChecks() {
        TestKit.suite("Crypto")
        val key = SecretBox.randomKey()
        val cipher = SecretBox.encrypt("sk-super-secret-key", key)
        TestKit.check(!cipher.contains("sk-super-secret-key"), "ciphertext does not leak the plaintext")
        TestKit.checkEquals("sk-super-secret-key", SecretBox.decrypt(cipher, key), "decrypt round-trip")

        val tampered = cipher.dropLast(4) + "AAAA"
        TestKit.check(SecretBox.decryptOrNull(tampered, key) == null, "tampered ciphertext is rejected")

        val otherKey = SecretBox.randomKey()
        TestKit.check(SecretBox.decryptOrNull(cipher, otherKey) == null, "wrong key cannot decrypt")

        val encoded = PasswordHasher.hash("correct horse battery staple".toCharArray())
        TestKit.check(encoded.startsWith("pbkdf2\$sha256\$"), "password hash uses PBKDF2-SHA256 format")
        TestKit.check(PasswordHasher.verify("correct horse battery staple".toCharArray(), encoded), "correct password verifies")
        TestKit.check(!PasswordHasher.verify("wrong password".toCharArray(), encoded), "wrong password rejected")
        TestKit.check(!PasswordHasher.verify("x".toCharArray(), "garbage"), "malformed hash rejected")
    }

    private fun registryChecks(providersJsonPath: String) {
        TestKit.suite("Provider registry (shared/providers.json)")
        val file = File(providersJsonPath)
        TestKit.check(file.exists(), "shared/providers.json exists at $providersJsonPath")
        if (!file.exists()) return
        val specs = ProviderRegistry.parse(file.readText())
        TestKit.checkEquals(8, specs.size, "8 providers are registered")
        val ids = specs.map { it.id }.toSet()
        TestKit.check(
            ids == setOf("gemini", "openai", "anthropic", "deepseek", "groq", "openrouter", "mistral", "custom"),
            "expected provider ids are present"
        )
        val gemini = specs.first { it.id == "gemini" }
        TestKit.checkEquals("x-goog-api-key", gemini.apiKeyHeader, "Gemini uses the x-goog-api-key header")
        val anthropic = specs.first { it.id == "anthropic" }
        TestKit.checkEquals("2023-06-01", anthropic.extraHeaders["anthropic-version"], "Anthropic version header present")
        val custom = specs.first { it.id == "custom" }
        TestKit.check(custom.customizable, "custom provider is marked customizable")
    }
}
