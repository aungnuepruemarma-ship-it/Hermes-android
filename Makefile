# Hermes Android — build helpers for GitHub Codespaces / local Gradle
# Usage:
#   make apk            build debug APK  -> app/build/outputs/apk/debug/app-debug.apk
#   make release        build signed release APK (needs HERMES_KEY_* env vars)
#   make install-adb    sideload debug APK via adb (needs a device/emulator)
#   make clean          remove build outputs
#   make test           run unit tests (if any)

GRADLE := ./gradlew
GRADLE_FLAGS := --no-daemon

.PHONY: apk release install-adb clean test

apk:
	$(GRADLE) $(GRADLE_FLAGS) assembleDebug

release:
	$(GRADLE) $(GRADLE_FLAGS) assembleRelease

install-adb:
	adb install -r app/build/outputs/apk/debug/app-debug.apk

clean:
	$(GRADLE) $(GRADLE_FLAGS) clean

test:
	$(GRADLE) $(GRADLE_FLAGS) testDebugUnitTest
