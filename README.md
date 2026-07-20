# Hermes for Android

A native Jetpack Compose front-end for the **existing** `hermes dashboard`
backend that runs inside Termux. The Android app does NOT reimplement the
Hermes runtime — it is a thin client for the FastAPI server already shipped
with `hermes-agent` (verified on 0.16.0).

## Architecture

```
Jetpack Compose app (this repo)
        │  HTTP + WebSocket, loopback only
        ▼
hermes dashboard  (FastAPI, in Termux)   ← already ships with hermes-agent
        │
Termux: hermes CLI · python · uv · SQLite · git · CLI tools
```

The app launches and supervises the dashboard via a foreground `Service`
(`HermesLauncherService`) that runs:

    hermes dashboard --host 127.0.0.1 --port 9119 --no-open

with `HERMES_DASHBOARD_SESSION_TOKEN` set in the child environment, so the
session token is deterministic and the app can authenticate without scraping
stdout.

## Prerequisites (on the device)

1. **Termux** installed (package name `com.termux` — if you use a fork, adjust
   `HermesLauncherService.findHermesBinary()`).
2. `hermes` on Termux's PATH and a working `hermes-agent` install
   (`hermes dashboard --help` should list `--host/--port/--no-open`).
3. The dashboard backend deps are already present in `hermes-agent`
   (fastapi, uvicorn). No extra install needed.
4. Android 8+ (foreground service). Android 14 needs the
   `FOREGROUND_SERVICE_DATA_SYNC` permission (already declared).

## Build & run (Android Studio)

1. Open this folder (`android/hermes`) as an Android project.
2. Let Gradle sync (downloads Gradle 8.9 via the wrapper).
3. `File ▸ Sync Project with Gradle Files`.
4. Connect a device/emulator with Termux + `hermes` installed.
5. Run `app`. On launch it starts the dashboard in Termux and renders the
   home screen once the token is read from `EncryptedSharedPreferences`.

> This project was authored without an Android SDK present, so it has not been
> compiled here. The Kotlin is internally consistent and every network call
> was verified against a live `hermes dashboard` instance. First build in
> Android Studio will resolve the version catalog and may download the Gradle
> distribution.

## Auth model (verified)

- Dashboard bound to **127.0.0.1** → no OAuth gate.
- REST: send header `X-Hermes-Session-Token: <token>` on every `/api/` route
  except `/api/status` (which is public).
- The client also pins `Host: 127.0.0.1:9119` (the backend rejects other Host
  values — DNS-rebinding defence, GHSA-ppp5-vxwm-4cf7).
- WebSocket (browser shell only): `?token=<token>` query param.

## Features / endpoints

| Screen         | Endpoint(s)                                                  |
|----------------|--------------------------------------------------------------|
| Home/status    | `/api/status`, `/api/system/stats`, gateway start/stop/restart |
| Tools          | `/api/tools/toolsets` + `PUT /api/tools/toolsets/{name}`    |
| Skills         | `/api/skills` + `PUT /api/skills/toggle`                    |
| Sessions       | `/api/sessions`, `/api/sessions/{id}/messages`              |
| Task queue     | `/api/cron/jobs` + trigger/pause/resume                     |
| Memory         | `/api/memory`, `POST /api/memory/reset`                     |
| Config         | `/api/config/raw` (GET/PUT, field `yaml_text`)              |
| Live output    | `/api/logs` (polled)                                        |

## Known backend gaps (hermes-agent 0.16.0)

- **WebSocket terminal is NOT available to external clients.** `/api/pty`
  returns 404 and `/api/ws` + `/api/pub` return 403 even with a valid token
  (they are gated for the browser/desktop shell). The "Live Output" screen
  therefore uses the REST `/api/logs` tail instead of a PTY stream. When the
  backend exposes a usable external PTY WS, swap `LogsScreen` for a
  `HermesWebSocket("pty")` view (the client already implements it).
- **No arbitrary file browser API.** Only the main config YAML is exposed
  (`/api/config/raw`). Project-file browsing would require an upstream
  sandboxed file API.

## Build in GitHub Codespaces (no local Android Studio)

This repo ships a `.devcontainer/` that installs JDK 17 + the Android SDK
(build-tools 34, platform 34) and generates the Gradle wrapper.

1. On the repo, click **Code → Codespaces → Create codespace on main**.
2. Wait for the container to build (installs SDK + `./gradlew`).
3. In the terminal:
   ```bash
   ./gradlew assembleDebug --no-daemon
   # APK: app/build/outputs/apk/debug/app-debug.apk
   ```
4. For a signed release, set the four env vars
   (`HERMES_KEYSTORE_PATH`, `HERMES_KEYSTORE_PASSWORD`, `HERMES_KEY_ALIAS`,
   `HERMES_KEY_PASSWORD`) and run `./gradlew assembleRelease`.
5. **Note:** the app talks to `hermes dashboard` over loopback inside Termux
   on a *real Android device*. In Codespaces there is no Termux/Hermes
   runtime, so the app builds and the API client compiles, but it cannot
   connect to a live backend here. Use Codespaces for building/signing the
   APK and editing code; test the running app on a phone with Termux.

A `Makefile` provides one-command builds:
```bash
make apk           # debug APK (auto-runs on Codespace creation)
make release       # signed release (needs HERMES_KEY_* env vars)
make install-adb   # sideload debug APK via adb
make clean         # wipe build outputs
```
## Project layout

```
android/hermes/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/                  # version catalog + wrapper (Gradle 8.9)
├── scripts/launch_dash.sh   # reference: how the token is injected
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── src/main/
    │   ├── AndroidManifest.xml
    │   ├── res/values/{strings,themes}.xml
    │   └── java/net/nous/hermes/
    │       ├── MainActivity.kt                 # NavHost + service start
    │       ├── net/
    │       │   ├── AuthInterceptor.kt          # token + Host header
    │       │   ├── HermesClient.kt             # Retrofit interface
    │       │   ├── HermesWebSocket.kt          # WS client (pty/ws/pub)
    │       │   └── models/HermesModels.kt      # DTOs (verified shapes)
    │       ├── service/
    │       │   ├── HermesLauncherService.kt    # foreground service
    │       │   └── EncryptedPrefs.kt           # token store
    │       └── ui/
    │           ├── HermesViewModel.kt          # state + actions
    │           └── Screens.kt                  # all composable screens
```

## Security notes

- Token is stored in `EncryptedSharedPreferences`, never plaintext or logs.
- Bind to loopback only; do not expose 0.0.0.0 without `--insecure` (engages
  an OAuth gate).
- The dashboard's PTY is a real shell — if/when the WS is enabled for external
  clients, gate it behind a confirm dialog.
