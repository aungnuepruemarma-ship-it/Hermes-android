#!/bin/bash
# Sideload the built debug APK to a connected device/emulator via adb.
#
# Usage:
#   ./scripts/install-apk.sh                 # installs latest debug APK
#   ./scripts/install-apk.sh path/to.apk     # installs a specific APK
#
# Requires `adb` on PATH and a device connected (USB debugging on, or an
# running emulator). The app package id is net.nous.hermes.
set -euo pipefail

APK="${1:-android/hermes/app/build/outputs/apk/debug/app-debug.apk}"

if ! command -v adb >/dev/null 2>&1; then
  echo "error: adb not found on PATH" >&2
  exit 1
fi

if [ ! -f "$APK" ]; then
  echo "error: APK not found: $APK" >&2
  echo "build it first (Android Studio or the GitHub Actions workflow)," >&2
  echo "or pass an explicit path: $0 /path/to/app-debug.apk" >&2
  exit 1
fi

echo "installing $APK ..."
adb install -r "$APK"
echo "done. Launch 'Hermes' on the device, or:"
echo "  adb shell am start -n net.nous.hermes/.MainActivity"
