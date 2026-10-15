#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "🔨 Building debug APK..."
./gradlew assembleDebug

APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK_PATH" ]]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi

echo "✅ Build succeeded: $APK_PATH"

DEVICE_SERIAL="${1:-}"
if [[ -z "$DEVICE_SERIAL" ]]; then
    DEVICE_SERIAL="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi

if [[ -n "$DEVICE_SERIAL" ]]; then
    echo "📲 Installing on device: $DEVICE_SERIAL..."
    adb -s "$DEVICE_SERIAL" install -r "$APK_PATH"
    echo "🚀 Launching application..."
    adb -s "$DEVICE_SERIAL" shell am start -n com.dailyvlog.app/.MainActivity
    echo "🎉 Application started successfully on $DEVICE_SERIAL!"
else
    echo "ℹ️ No connected Android device found. APK built at: $APK_PATH"
fi
