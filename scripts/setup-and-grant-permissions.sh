#!/usr/bin/env bash
# OmniAgent ADB Auto-Permission Grant Script
set -e

PACKAGE="com.omniagent.assistant"
SERVICE="com.omniagent.assistant.service.AgentAccessibilityService"

echo "=== [OmniAgent] Granting Android OS Permissions via ADB ==="

echo "1. Granting RECORD_AUDIO..."
adb shell pm grant "$PACKAGE" android.permission.RECORD_AUDIO || true

echo "2. Granting SYSTEM_ALERT_WINDOW (Floating Island & Overlay)..."
adb shell appops set "$PACKAGE" SYSTEM_ALERT_WINDOW allow || true

echo "3. Enabling Accessibility Service..."
adb shell settings put secure enabled_accessibility_services "$PACKAGE/$SERVICE" || true
adb shell settings put secure accessibility_enabled 1 || true

echo "4. Disabling Battery Optimization (Doze Mode bypass)..."
adb shell dumpsys deviceidle whitelist +"$PACKAGE" || true

echo "=== All permissions successfully granted! Launching OmniAgent... ==="
adb shell am start -n "$PACKAGE/.ui.MainActivity"
