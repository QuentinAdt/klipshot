#!/bin/sh
# Builds the debug APK.
#
# Requirements: a JDK 17 and the Android SDK (platform 36, build-tools 36).
# Set JAVA_HOME and ANDROID_HOME yourself, or let the Homebrew defaults below apply.
set -e
[ -z "$JAVA_HOME" ] && [ -d /opt/homebrew/opt/openjdk@17 ] && export JAVA_HOME=/opt/homebrew/opt/openjdk@17
[ -z "$ANDROID_HOME" ] && [ -d /opt/homebrew/share/android-commandlinetools ] && export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
cd "$(dirname "$0")"
./gradlew "${@:-assembleDebug}"
echo
echo "APK: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
