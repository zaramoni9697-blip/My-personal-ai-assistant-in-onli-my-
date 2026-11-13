# JARVIS AI

A work-in-progress personal assistant for Md Foysal. Android is the primary target; a web application is planned. **This repository is not ready for deployment or use with real credentials.**

## Current status

- `android/app/src/main/java/com/mdfoysal/jarvis/core/`: Kotlin JSON, provider adapters, chat, language/persona and cryptography. The JVM verification harness exercises these without an Android SDK.
- `android/app/src/main/java/com/mdfoysal/jarvis/`: an Android application layer is drafted, but has **not been compiled or run**. UI and device features must not be treated as verified.
- `web/server/`: incomplete backend draft; there is no server entry point or web client. It is not deployable. Password reset is disabled until secure delivery is implemented.
- `shared/providers.json`: shared provider metadata. Model availability can change and must be checked with providers.
- `tools/kotlin-verify/`: standalone JVM checks, including mocked backend-auth client responses.

## Verification

The core verification script requires a JDK and Kotlin compiler libraries. Set `KC_LIB` to a directory containing `kotlin-compiler.jar`, `kotlin-stdlib.jar`, `kotlin-reflect.jar`, and `kotlinx-coroutines-core-jvm.jar`, and make `java` available on `PATH`:

```sh
KC_LIB=/path/to/kotlin/lib bash tools/kotlin-verify/run.sh
```

Web draft security regression test (Node 22.5+; Node's SQLite API is experimental):

```sh
cd web/server
npm ci
npm test
```

These tests do not verify a complete Android app or a working web service, nor do they contact live AI providers. No APK has been built.

### Build a debug APK with GitHub Actions

`.github/workflows/build-apk.yml` runs on relevant pushes and pull requests, and can also be started manually from **Actions → Build Android debug APK → Run workflow** after the workflow is pushed to GitHub. If the build succeeds, download the `jarvis-ai-debug-apk` artifact from the run (available for 14 days). The APK is debug-signed and is not suitable for publishing. The Android app has not yet been compiled; the workflow may expose source or configuration errors that must be fixed before an APK is produced.

## Blocking work before release

1. Install a compatible JDK, Gradle and Android SDK, add a Gradle wrapper, compile and test the Android app on devices; correct remaining source/dependency and resource errors. Release APK signing must use a private deployment key, not the debug key.
2. Complete and test authentication and session security, account deletion, user data isolation and password reset. Local-only accounts cannot currently be recovered if the password is lost. Remote tokens currently need secure storage and revocation.
3. Complete and validate provider setup, history, image input, settings and voice flows on Android. Wake-word settings do not currently start a working listener; background activity launch and microphone handoff need an Android-compliant, consent-based design. The Android transport currently buffers streamed responses.
4. Design and complete the web server and responsive client. Before enabling outbound provider requests, implement and test SSRF defenses (including DNS resolution, redirects, private addresses and custom endpoints), credential-safe error handling, HTTPS, session revocation, rate limiting, and user isolation. Current web draft is not safe for internet-facing deployment.
5. Test every provider adapter and failure path, permission denial and lifecycle cases, then build and verify an APK. Real provider credentials and Android device/emulator testing are required for end-to-end verification.
