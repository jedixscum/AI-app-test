# PocketDev AI — Milestone 1

Foundation of an Android app that turns a phone into a development workstation by driving **Termux**
(no root). This milestone covers the Termux connection, automated environment setup, the code-server
editor in a WebView, diagnostics, logs and persistent settings.

## Status: written, NOT yet compiled or run

This code was written in a sandbox with no Android SDK, no Gradle and no network. **Nothing here has
been built, unit-tested or run on a device.** Treat the first build as the first test.

First steps:
1. Push to GitHub: `.github/workflows/build.yml` runs unit tests and builds a debug APK (artifact `pocketdev-debug-apk`).
   Or open the folder in Android Studio (it will fetch Gradle 8.9 from `gradle-wrapper.properties`)
   or run `gradle :app:testDebugUnitTest :app:assembleDebug`.
2. Fix whatever the compiler reports. Plugin and library versions were chosen from memory and may need bumping.
3. Install the APK, then work through the device checklist below.

## Device checklist (nothing below is verified yet)
- [ ] Termux installed from F-Droid/GitHub (not Play Store); app shows "Permission needed", grant it.
- [ ] After `allow-external-apps=true` and a Termux restart, "Test connection" shows Connected.
- [ ] Setup: base tools, Ubuntu, code-server, secure config each reach "Ready" (verified by a separate check).
- [ ] Start editor; code-server is healthy on 127.0.0.1:8080; WebView logs in and opens a folder.
- [ ] New project creates `~/projects/<name>` and it appears in the editor's file tree.
- [ ] Diagnostics screen reports real pass/fail results; Logs export contains no password.
- [ ] Settings: Ollama check reports reachable/unreachable correctly; remote URLs show the "Remote" warning.
- [ ] Settings and logs survive an app restart.
- [ ] Keep-alive guide: test process is still alive after ~30 min with the screen off; the fix steps shown match your Android version.

## Assumptions to verify (written from documentation memory)
- Termux `RUN_COMMAND` extras and the result-bundle keys (`stdout`, `stderr`, `exitCode`, `err`, `errmsg`) in `TermuxBridge.kt`.
- `code-server` is installable with `pkg install tur-repo && pkg install code-server`. If not, change only `assets/setup/install_codeserver.sh`.
- code-server accepts `POST /login?to=...` with a `password` field (used by the WebView auto-login) and serves `/healthz`.
- Background processes started with `nohup` survive after the RUN_COMMAND session ends (a wake lock is requested).

## Security choices
- code-server binds to 127.0.0.1 with a generated password (mode 600 config). The password is held in memory only and redacted from logs.
- Cleartext HTTP is allowed only for 127.0.0.1/localhost (`network_security_config.xml`).
- Project names are validated and shell-quoted before use in commands.
- The "Local/Remote" label is derived from the endpoint host, so a non-loopback Ollama URL is shown as Remote.

## Known limits of this milestone
- Termux returns output only when a command ends; long installs show progress by polling a log file while the app is open.
- No cancel button for running installs (commands keep running inside Termux).
- No API-key storage yet (needed before remote providers can be used; planned with Android Keystore).
- Launcher icon is the system placeholder.
- Keep-alive guide only *tests* and *explains*; it applies no settings. The Android 12/13 `device_config` command and the Android 14+ developer toggle name are from documentation memory and unverified.

## Layout
- `core/` pure Kotlin models and helpers (unit-tested)
- `termux/` RUN_COMMAND bridge, result receiver, detection
- `assets/setup/` idempotent setup, check, start/stop scripts
- `ai/` provider abstraction (Ollama, OpenAI-compatible) and coding-agent seam
- `data/` DataStore settings and persistent redacted log
- `EnvironmentRepository.kt`, `AppViewModel.kt`, `ui/` state and Compose screens

## Next milestones
Ollama install and model management; coding agent with multi-file edits and diff review; GitHub auth and clone;
project templates; build/test/export; additional execution backends.
