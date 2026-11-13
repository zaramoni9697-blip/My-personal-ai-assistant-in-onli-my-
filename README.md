# JARVIS AI — Advanced Personal AI Assistant by Md Foysal

> **A futuristic, production-oriented Personal AI Assistant inspired by JARVIS, built for Md Foysal.**  
> Android (primary) + Responsive Web Application • Bengali & English • Voice-first • Multi-Provider AI

---

## ✨ Overview

**JARVIS AI** is not a simple chatbot. It is a complete AI assistant platform with:

- 🔐 **Secure Auth** — Login / Signup / Forgot Password with encrypted local storage
- 🔌 **Multi-Provider AI** — Gemini, OpenAI, Claude, DeepSeek, Groq, OpenRouter, Mistral, Custom API with per-user isolation
- 🎙️ **Voice Assistant** — Speech-to-Text, Text-to-Speech, Bengali/English auto-detection, wake word `সবসময় প্রস্তুত` (foreground service compliant)
- 📱 **Device Control** — Phone control via Android intents (apps, alarms, volume, flashlight, battery, settings) with permission explanations
- 💬 **Intelligent Chat** — Context-aware conversations, conversation history, memory management, coding/math/translation/summarization
- 🎨 **Futuristic UI** — Dark holographic blue theme, animated AI core, voice activity visualizations
- 🌐 **Bilingual Personality** — Friendly, professional, humorous; correctly answers *“Who created you?” → “I was created by Md Foysal.”* in both languages

---

## 🏗️ Architecture

### Android (Kotlin + Jetpack Compose)
```
android/
├── app/src/main/java/com/jarvis/ai/
│   ├── JarvisApplication.kt          # Hilt app
│   ├── MainActivity.kt
│   ├── navigation/NavGraph.kt
│   ├── data/local/db/JarvisDatabase.kt  # Room: users, providers, conversations, messages
│   ├── data/local/datastore/PreferencesManager.kt # DataStore
│   ├── data/security/CryptoManager.kt   # EncryptedSharedPreferences + Android Keystore
│   ├── data/repository/                 # Auth, Provider, Chat repositories
│   ├── network/provider/ProviderAdapters.kt # 8 provider adapters (Gemini, OpenAI-compat, Claude)
│   ├── voice/                           # STT, TTS, WakeWordService (foreground service)
│   ├── device/                          # DeviceController + PermissionManager
│   ├── viewmodel/                       # Auth, Provider, Chat, Voice, Device VMs
│   ├── ui/theme/                        # JarvisTheme (dark holographic)
│   └── ui/screens/                      # Splash, Auth, Config, Dashboard, Chat, Voice, Device, Settings
└── build.gradle.kts                     # Compose BOM, Hilt, Room, Security, Retrofit, etc.
```

**Key Libraries:**
- Jetpack Compose + Material3 + Navigation Compose
- Hilt (DI), Room, DataStore, Security-Crypto (EncryptedSharedPreferences), Biometric
- Retrofit + OkHttp + Gson, Coroutines, Coil, Lottie, Accompanist Permissions

### Web (React + Vite + TypeScript + Tailwind)
```
web/
├── src/
│   ├── components/   # AICore, Layout (TopBar/BottomNav)
│   ├── pages/        # Splash, Auth, Providers, Dashboard, Chat, Voice, Device, Settings
│   ├── services/     # providerAdapters, storage
│   ├── store/        # Zustand global store (per-user persistence)
│   ├── types/        # ProviderConfig, Conversation, etc.
│   └── utils/        # lang detection, fallback replies
├── vite.config.ts
└── tailwind.config.js
```

---

## 🚀 Quick Start

### Web App (Ready to Run)
```bash
cd web
npm install
npm run dev      # http://localhost:5173
npm run build    # production build → dist/
```

**Live Preview:** The dev server runs on `0.0.0.0:5173` and is proxied for the platform preview. It is already configured with `allowedHosts: true`.

**Test Accounts (demo auth stored in localStorage):**
- Sign up with any email/password (≥6 chars) → creates local user
- Login validates against `localStorage['jarvis:users']`

### Android App
Requirements: Android Studio Hedgehog+, JDK 17, SDK 34.

```bash
# Open android/ in Android Studio
# Add google-services.json for Firebase (optional - local auth works without it)
# Sync Gradle & Run on device/emulator

# Or via CLI (if SDK installed):
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Build Notes:**
- `minSdk 26`, `targetSdk 34`, Compose `1.5.8`, Kotlin `1.9.22`
- Encrypted storage uses `androidx.security:security-crypto` with `MasterKey.AES256_GCM`
- All provider keys stored per-user and never hardcoded

---

## 🔑 Provider Configuration

After login, you land on **Provider Configuration**:

1. Tap `+` → select provider (Gemini, OpenAI, Claude, DeepSeek, Groq, OpenRouter, Mistral, Custom)
2. Paste **API Key** (show/hide toggle), enter **Model ID** (pre-filled), **Base URL** (editable), **Display Name**
3. **Test Connection** → shows success/error from real adapter
4. **Save** → encrypted locally; **Set as Active** to use for chat
5. You can add multiple providers and switch active one; errors show clear messages and allow fallback

**Provider Adapters:**
- `OpenAICompatibleAdapter` handles OpenAI, DeepSeek, Groq, OpenRouter, Mistral, Custom (Bearer token, `/chat/completions`)
- `GeminiAdapter` handles Gemini (`:generateContent` with systemInstruction)
- `ClaudeAdapter` handles Anthropic (`/messages` with `x-api-key` + `anthropic-version`)
- Factory pattern: `ProviderAdapterFactory.get(type)`

> **Security:** Keys stored in `EncryptedSharedPreferences` (Android) or namespaced `localStorage` per `userId` (Web). Never exposed in logs/frontend source. Network uses HTTPS only.

---

## 🎙️ Voice Features

- **STT:** `SpeechRecognizer` (Android) / `webkitSpeechRecognition` (Web) with `bn-BD`/`en-US` + auto-detection
- **TTS:** `TextToSpeech` (Android) / `speechSynthesis` (Web) with Bengali locale detection (Unicode range `0980-09FF`)
- **Wake Word:** `সবসময় প্রস্তুত` — implemented as **foreground service** with visible notification (`WakeWordService`, `FOREGROUND_SERVICE_MICROPHONE`), not secret recording
- **Controls:** Listen/Stop, Speak/Stop, interrupt current response, language selector (Auto/EN/BN)

---

## 📱 Device Control (Android)

All actions use **user-approved intents** and request confirmation:

| Feature | API |
|---------|-----|
| Open apps | `PackageManager.getLaunchIntentForPackage` |
| Alarms | `AlarmClock.ACTION_SET_ALARM` |
| Volume | `AudioManager.setStreamVolume` |
| Flashlight | `CameraManager.setTorchMode` |
| Battery | `BatteryManager` |
| Device info | `Build.*` |
| Calls/SMS | `Intent.ACTION_DIAL` / `ACTION_SENDTO` |
| Settings | `Settings.ACTION_*` |
| Installed apps | `getInstalledApplications` filtered by launch intent |

Permission page (`DeviceControlScreen`) explains every permission; no bypass of Android safeguards.

---

## 🧠 AI Intelligence

- System prompt enforces JARVIS persona + creator identity
- Handles general chat, coding, math, translation, summarization, writing, problem solving
- Context-aware (loads last messages), conversation history with delete/clear
- Image understanding ready where provider supports multimodal (adapter extensible)
- Local fallback for offline: detects Bengali/English and answers creator questions correctly

---

## 🔒 Security

- Per-user data isolation (userId-namespaced DB + prefs)
- Encrypted API key storage (Android Keystore + EncryptedSharedPreferences)
- Input validation (email regex, password length, required fields)
- Network error handling with user-friendly messages
- Permission management (accompanist + runtime checks)
- Logout / Delete Account with data wipe
- `usesCleartextTraffic="false"`, R8 full mode, ProGuard rules

---

## 🎨 UI Design

- **Colors:** `JarvisBackground #0A0E1A`, `JarvisBlue #00D4FF`, `JarvisAccent #00FFD1`, `JarvisSurface #0F1A2E`
- **Dashboard:** Animated circular AI core (rotating dots, pulse, voice bars), status chips, quick actions, suggestions
- **Chat:** Bubble UI with provider/model attribution
- **Responsive:** Web shell `max-w-[480px]` mobile → `lg:max-w-6xl` desktop, bottom nav on mobile

---

## 📂 File Paths (Complete)

**Android:**
- `android/app/src/main/java/com/jarvis/ai/MainActivity.kt`
- `android/app/src/main/java/com/jarvis/ai/JarvisApplication.kt`
- `android/app/src/main/java/com/jarvis/ai/navigation/NavGraph.kt`
- `android/app/src/main/java/com/jarvis/ai/data/local/db/JarvisDatabase.kt`
- `android/app/src/main/java/com/jarvis/ai/data/security/CryptoManager.kt`
- `android/app/src/main/java/com/jarvis/ai/data/local/datastore/PreferencesManager.kt`
- `android/app/src/main/java/com/jarvis/ai/data/repository/AuthRepository.kt`
- `android/app/src/main/java/com/jarvis/ai/data/repository/ProviderRepository.kt`
- `android/app/src/main/java/com/jarvis/ai/data/repository/ChatRepository.kt`
- `android/app/src/main/java/com/jarvis/ai/network/provider/ProviderAdapters.kt`
- `android/app/src/main/java/com/jarvis/ai/voice/SpeechRecognitionManager.kt`
- `android/app/src/main/java/com/jarvis/ai/voice/TextToSpeechManager.kt`
- `android/app/src/main/java/com/jarvis/ai/voice/WakeWordService.kt`
- `android/app/src/main/java/com/jarvis/ai/device/DeviceController.kt`
- `android/app/src/main/java/com/jarvis/ai/device/PermissionManager.kt`
- `android/app/src/main/java/com/jarvis/ai/di/AppModule.kt`
- `android/app/src/main/java/com/jarvis/ai/viewmodel/AuthViewModel.kt`
- `android/app/src/main/java/com/jarvis/ai/viewmodel/ProviderViewModel.kt`
- `android/app/src/main/java/com/jarvis/ai/viewmodel/ChatViewModel.kt`
- `android/app/src/main/java/com/jarvis/ai/viewmodel/VoiceViewModel.kt`
- `android/app/src/main/java/com/jarvis/ai/viewmodel/DeviceViewModel.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/theme/Theme.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/theme/Color.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/screens/splash/SplashScreen.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/screens/auth/LoginScreen.kt` (contains Login, Signup, Forgot)
- `android/app/src/main/java/com/jarvis/ai/ui/screens/config/ProviderConfigScreen.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/screens/dashboard/DashboardScreen.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/screens/chat/ChatScreen.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/screens/voice/VoiceAssistantScreen.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/screens/device/DeviceControlScreen.kt`
- `android/app/src/main/java/com/jarvis/ai/ui/screens/settings/SettingsScreen.kt`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/build.gradle.kts`, `android/build.gradle.kts`, `android/settings.gradle.kts`

**Web:**
- `web/src/main.tsx`, `web/src/App.tsx`, `web/src/index.css`
- `web/src/components/AICore.tsx`, `web/src/components/Layout.tsx`
- `web/src/pages/Splash.tsx`, `web/src/pages/Auth.tsx`, `web/src/pages/Providers.tsx`, `web/src/pages/Dashboard.tsx`, `web/src/pages/Chat.tsx`, `web/src/pages/Voice.tsx`, `web/src/pages/Device.tsx`, `web/src/pages/Settings.tsx`
- `web/src/services/providerAdapters.ts`, `web/src/services/storage.ts`
- `web/src/store/useStore.ts`, `web/src/types/index.ts`, `web/src/utils/lang.ts`

---

## ✅ Verification

- Web `npm run build` passes (`tsc && vite build`)
- Dev server runs on `0.0.0.0:5173` with `allowedHosts: true`
- Creator identity tested: “Who created you?” / “তোমাকে কে তৈরি করেছে?” → correct in both languages
- Provider adapters use provider-specific formats, not one-shape-fits-all
- No hardcoded API keys

---

## 📄 License & Credits

Created for **Md Foysal**. Original branding (not Marvel-owned). JARVIS concept inspiration only.

---

## 🔜 Roadmap

- Firebase Auth + Firestore sync (optional)
- On-device wake-word ML (PocketSphinx / TensorFlow Lite)
- Image upload for multimodal providers (Gemini Vision, GPT-4o)
- Wear OS companion

