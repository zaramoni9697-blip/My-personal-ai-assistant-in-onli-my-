# Android — JARVIS AI

See root `README.md` for overview.

## Build

1. Open `android/` in Android Studio Hedgehog+ (JDK 17, SDK 34)
2. Place `google-services.json` in `android/app/` if using Firebase Auth (optional; local auth works without)
3. Sync Gradle, Run on device.

CLI:
```bash
./gradlew assembleDebug
```

## Structure
- `data/local/db` — Room DB
- `data/security` — EncryptedSharedPreferences
- `network/provider` — 8 adapters
- `voice` — STT/TTS + WakeWord foreground service
- `device` — intent-based phone control
- `ui/screens/*` — Compose screens

## Permissions
Declared in `AndroidManifest.xml`; runtime handling via Accompanist + PermissionManager. Wake word uses `FOREGROUND_SERVICE_MICROPHONE` with notification.

## Security
- MasterKey AES256_GCM
- Per-user isolation
- No hardcoded keys
