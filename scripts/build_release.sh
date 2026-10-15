#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "🔨 Building signed release APK..."
./gradlew assembleRelease

RELEASE_APK="$PROJECT_ROOT/app/build/outputs/apk/release/app-release.apk"

if [[ ! -f "$RELEASE_APK" ]]; then
    echo "❌ Release APK not found at $RELEASE_APK"
    exit 1
fi

APK_SIZE="$(du -h "$RELEASE_APK" | cut -f1)"
echo "✅ Release APK built successfully!"
echo "📦 Path: $RELEASE_APK ($APK_SIZE)"

DEVICE_SERIAL="${1:-}"
if [[ -z "$DEVICE_SERIAL" ]]; then
    DEVICE_SERIAL="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi

if [[ -n "$DEVICE_SERIAL" ]]; then
    read -rp "📲 Device $DEVICE_SERIAL detected. Install release APK now? [y/N] " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        echo "📲 Installing release APK on $DEVICE_SERIAL..."
        adb -s "$DEVICE_SERIAL" install -r "$RELEASE_APK"
        echo "🚀 Launching application..."
        adb -s "$DEVICE_SERIAL" shell am start -n com.dailyvlog.app/.MainActivity
        echo "🎉 Release APK running on $DEVICE_SERIAL!"
    fi
fi
