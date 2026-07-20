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

1. Open this repository folder as an Android project (the Android app lives at
   the **repo root**, not under `android/hermes/`).
2. Let Gradle sync (the CI uses Gradle 8.9; locally provide a wrapper or set
   `gradle-version`).
3. `File ▸ Sync Project with Gradle Files`.
4. Connect a device/emulator with Termux + `hermes` installed.
5. Run `app`. On launch it starts the dashboard in Termux and renders the
   home screen once the token is read from `EncryptedSharedPreferences`.

> This project was authored without an Android SDK present, so it was not
> compiled locally. It IS compiled and verified by the GitHub Actions CI
> (`.github/workflows/build.yml`), which builds a debug APK on every push.
> First local build in Android Studio will resolve the version catalog and
> download the Gradle distribution.

## Get the APK (no Android Studio needed)

The CI builds a debug APK automatically on every push to `main`.

1. Open **Actions → "Build Android APK"** on the repo.
2. Open the latest run → **Artifacts** → download `hermes-android-debug`.
3. Install on your device:
   ```bash
   adb install -r app-debug.apk        # from a PC with adb
   # or copy app-debug.apk to Downloads and tap it (enable "Install unknown apps")
   ```
4. In Termux, start the backend the app expects:
   ```bash
   hermes dashboard --host 127.0.0.1 --port 9119 --no-open
   ```
5. Open the Hermes app, grant the foreground-service permission, and it
   connects over loopback (127.0.0.1:9119).

## Signed release (CI)

The `release` job in the workflow builds a **signed** APK, but only when the
repo has these four GitHub Actions secrets set (Settings → Secrets and
variables → Actions → New repository secret):

| Secret              | Value                                        |
|---------------------|----------------------------------------------|
| `KEYSTORE_BASE64`   | base64 of a PKCS12 keystore                  |
| `KEYSTORE_PASSWORD` | keystore password                            |
| `KEY_ALIAS`         | key alias (e.g. `hermes`)                    |
| `KEY_PASSWORD`      | key password                                 |

When all four are present, the release job runs `assembleRelease` and uploads
`hermes-android-release`. Without them, the release job is a no-op and only the
debug APK is produced. (A sample keystore can be generated with the script in
`scripts/gen_keystore.py` if you need a throwaway one for testing — do not
commit the keystore or its password.)

### Tag a release (publish a GitHub Release)

`.github/workflows/release.yml` builds the **signed** APK and publishes it as a
GitHub Release whenever you push a `v*` tag:

```bash
git tag v1.0.0
git push origin v1.0.0        # triggers release.yml -> signed APK + GitHub Release
```

This requires the four secrets above to be set; otherwise the job fails fast
with a clear "Missing signing secrets" error.

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

## Build in GitHub Actions (no local Android Studio)

This repo ships `.github/workflows/build.yml`, which builds the debug APK on
every push/PR (and via `workflow_dispatch`). It provisions JDK 17 + Android
SDK 34 and Gradle 8.9 — no committed wrapper needed. The signed release job
runs only when the four secrets above are set.

You do not need Codespaces or a local SDK to obtain a working APK — just pull
the `hermes-android-debug` artifact from the latest Actions run (see "Get the
APK" above).

A `Makefile` provides one-command builds for a local environment that *does*
have the Android SDK:

```bash
make apk           # debug APK
make release       # signed release (needs HERMES_KEY_* env vars)
make install-adb   # sideload debug APK via adb
make clean         # wipe build outputs
```

> A `.devcontainer/` (JDK17 + Android SDK) is still included for editor
> convenience, but Codespaces creation for this repo requires `codespace`
> scope + repo admin, which is gated on the GitHub side — the Actions path
> above is the reliable one.
## Project layout

```
.                               # repo root = Android project root
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/                     # version catalog + wrapper.properties (Gradle 8.9)
├── Makefile
├── scripts/launch_dash.sh      # reference: how the token is injected
├── .github/workflows/build.yml # CI: debug + (optional) signed release
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/values/{strings,themes}.xml
        └── java/net/nous/hermes/
            ├── MainActivity.kt                 # NavHost + service start
            ├── net/
            │   ├── AuthInterceptor.kt          # token + Host header
            │   ├── HermesClient.kt             # Retrofit interface
            │   ├── HermesWebSocket.kt          # WS client (pty/ws/pub)
            │   └── models/HermesModels.kt      # DTOs (verified shapes)
            ├── service/
            │   ├── HermesLauncherService.kt    # foreground service
            │   └── EncryptedPrefs.kt           # token store
            └── ui/
                ├── HermesViewModel.kt          # state + actions
                └── Screens.kt                  # all composable screens
```

## Security notes

- Token is stored in `EncryptedSharedPreferences`, never plaintext or logs.
- Bind to loopback only; do not expose 0.0.0.0 without `--insecure` (engages
  an OAuth gate).
- The dashboard's PTY is a real shell — if/when the WS is enabled for external
  clients, gate it behind a confirm dialog.
