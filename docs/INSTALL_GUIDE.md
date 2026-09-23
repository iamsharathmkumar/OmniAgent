# OmniAgent AI: Android APK Installation & Quickstart Guide

The compiled, ready-to-install Android package **`OmniAgent-v1.0.apk`** has been generated and signed with Android Signature Scheme v2 & v3.

---

## 1. Where to Get Your APK

- **Direct Download from Preview Web Studio**: Click the green **"Download APK"** button at the top of the live preview at port 3000.
- **Local File in Workspace**: Located at `/home/user/OmniAgent-v1.0.apk` (and `/home/user/apk-build/bin/OmniAgent-v1.0.apk`).

---

## 2. How to Install on Any Android Device

### Option A: Install via ADB (Fastest)
If your phone is plugged in with USB Debugging enabled:
```bash
# 1. Install the APK
adb install -r OmniAgent-v1.0.apk

# 2. Automatically grant system permissions (Audio, Overlay & Accessibility)
adb shell pm grant com.omniagent.assistant android.permission.RECORD_AUDIO
adb shell appops set com.omniagent.assistant SYSTEM_ALERT_WINDOW allow
adb shell settings put secure enabled_accessibility_services com.omniagent.assistant/com.omniagent.assistant.service.AgentAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell dumpsys deviceidle whitelist +com.omniagent.assistant

# 3. Launch the app
adb shell am start -n com.omniagent.assistant/.ui.MainActivity
```

### Option B: Direct Sideloading (Phone Only)
1. Download `OmniAgent-v1.0.apk` to your phone (via browser, Google Drive, WhatsApp, or cable transfer).
2. Tap the downloaded file in your Android **Files** app.
3. If prompted with *"Install unknown apps"*, toggle **Allow from this source**.
4. Tap **Install** and open **OmniAgent AI**.

---

## 3. Initial App Permissions Setup (In-App Wizard)

When you launch OmniAgent, follow the 3 quick steps on the dashboard:

1. **Accessibility Service**:
   - Tap **"1. Enable Accessibility in Settings"**.
   - Select **OmniAgent AI** and toggle it **ON**.
   - *(This allows the agent to read screen layout nodes and execute taps and typing).*
2. **Draw Over Other Apps**:
   - Tap **"2. Grant Draw Over Other Apps"**.
   - Enable permission for **OmniAgent AI**.
   - *(This enables the persistent floating Dynamic Island microphone HUD).*
3. **Microphone**:
   - Tap **"3. Grant Microphone Permission"** and allow audio recording.

---

## 4. How to Use Voice Commands

### Using the Floating HUD
- Tap the **"Launch HUD"** button.
- A floating draggable pill appears on your screen over all apps:
  - Tap the **🎤 Mic** icon anytime to speak a command.
  - Watch the live reasoning status (e.g., *"Opening WhatsApp..."*, *"Typing message..."*, *"Tapping Send"*).
  - Tap the **🛑 Stop** button at any second to halt autonomous execution.

### Try These Natural Voice Commands:
- *"Send a message to Alex on WhatsApp saying I'll be 10 minutes late"*
- *"Order a Margherita pizza on DoorDash"*
- *"Book a Comfort ride to Central Station on Uber"*
- *"Set an alarm for 7:00 AM tomorrow"*
- *"Open Settings and turn on Dark Mode"*
- *"Search for latest technology news on Chrome"*

---

## 5. APK Specifications

| Property | Value |
|---|---|
| **Package Name** | `com.omniagent.assistant` |
| **Version** | `1.0.0` (Code 1) |
| **Minimum SDK** | `API 26` (Android 8.0 Oreo) |
| **Target SDK** | `API 34` (Android 14) |
| **Signatures** | `APK Signature Scheme v2 & v3` (Active) |
| **Size** | `~33 KB` (Zero bloat, pure native Android) |
| **Components** | `MainActivity`, `AgentAccessibilityService`, `VoiceAssistantService`, `FloatingOverlayService` |
