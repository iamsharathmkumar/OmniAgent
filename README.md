# 🤖 OmniAgent AI: Autonomous Android OS Voice Agent

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%E2%80%9334)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)](https://kotlinlang.org)
[![VLA Agent](https://img.shields.io/badge/Architecture-ReAct%20%7C%20Vision--Language--Action-orange.svg)]()

> **OmniAgent** is a system-level agentic assistant for Android that performs any task across any installed mobile application through simple, natural voice commands.

Instead of needing custom API integrations for every app, OmniAgent acts as an autonomous digital user: it listens to voice commands, inspects the on-screen UI hierarchy, reasons via Multimodal LLMs, and executes synthetic touches, gestures, text inputs, and system navigation directly on the Android OS.

---

## 🌟 Key Features

- 🎙️ **Hands-Free Voice Control**: Persistent background foreground service with continuous speech recognition (`SpeechRecognizer`) and spoken Text-to-Speech replies (`TextToSpeech`).
- 👁️ **Real-Time Screen Grounding**: Parses active Android window widgets into token-efficient UI elements using `AccessibilityNodeInfo` tree traversal and bounds detection.
- 🧠 **Multi-Hop Agentic Reasoning**: Implements the ReAct (Observe → Reason → Act → Verify) loop powered by OpenAI (GPT-4o), Anthropic (Claude 3.5 Sonnet), and Google Gemini (Gemini 2.0 Flash).
- 👆 **OS Synthetic Action Engine**: Dispatches taps, drags, scrolls, swipes via `dispatchGesture()`, inputs text via `ACTION_SET_TEXT`, and triggers system actions (Back, Home, Recents).
- 🏝️ **Floating Dynamic Island HUD**: Draggable overlay pill drawn on top of all apps (`SYSTEM_ALERT_WINDOW`) displaying the agent's live thoughts and an immediate emergency stop kill-switch.
- 🛡️ **Sensitive Action Guardrails**: Intercepts financial transactions, checkout buttons, and irreversible operations with explicit user confirmation.
- 🧪 **Interactive Web Simulator**: Built-in testbed studio with virtual phone simulation, live accessibility tree inspector, and code hub.

---

## 📱 Repository Structure

```
OmniAgent/
├── android/                   # Production Android Studio Kotlin Project
│   ├── app/
│   │   ├── src/main/java/com/omniagent/assistant/
│   │   │   ├── agent/         # ReAct Orchestrator & LLM Client
│   │   │   ├── service/       # AccessibilityService, VoiceService, FloatingOverlay
│   │   │   ├── model/         # UI Elements & Action Decision Schema
│   │   │   └── ui/            # Compose Dashboard & Permission Wizard
│   │   ├── res/               # Layouts, XML configs, strings
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── releases/                  # Pre-compiled, signed Android APK
│   └── OmniAgent-v1.0.apk     # Ready to install on Android 8.0 - 15
├── web-simulator/             # Vite + React Interactive Virtual Phone Studio
├── scripts/                   # Automated ADB permission setup script
│   └── setup-and-grant-permissions.sh
└── docs/                      # Technical documentation
    ├── ARCHITECTURE.md
    └── INSTALL_GUIDE.md
```

---

## ⚡ Quick Start: Installing the APK

### Direct Sideload
1. Download **`https://github.com/iamsharathmkumar/OmniAgent/releases/download/v1.0.0/OmniAgent-v1.0.apk`** to your phone.
2. Tap the APK file and allow **"Install unknown apps"**.
3. Open **OmniAgent AI** and follow the 3-step permission setup.

### Automated ADB Setup (1 Command)
```bash
# 1. Install APK
adb install -r https://github.com/iamsharathmkumar/OmniAgent/releases/download/v1.0.0/OmniAgent-v1.0.apk

# 2. Grant OS Permissions & Accessibility Engine
./scripts/setup-and-grant-permissions.sh
```

---

## 🗣️ Example Voice Commands

| Category | Example Voice Command | What OmniAgent Does |
|---|---|---|
| **Messaging** | *"Send a message to Alex on WhatsApp saying I'm 10m late"* | Opens WhatsApp, finds Alex Rivera, types message, taps Send |
| **Food Delivery** | *"Order a Margherita pizza on DoorDash"* | Launches DoorDash, selects pizzeria, adds item, prompts for payment approval |
| **Ride Sharing** | *"Book an Uber Comfort to the airport"* | Launches Uber, enters destination, selects Comfort tier, confirms ride |
| **Alarms & Clock** | *"Set an alarm for 7:00 AM tomorrow"* | Opens Clock app, taps '+', sets time picker, saves alarm |
| **Device Settings** | *"Open Settings and turn on Dark Mode"* | Launches Settings, navigates to Display, toggles Dark Theme |

---

## 🛠️ Building From Source

1. Clone this repository:
   ```bash
   git clone https://github.com/<your-username>/OmniAgent.git
   cd OmniAgent/android
   ```
2. Open the `android` folder in **Android Studio Jellyfish / Koala** or newer.
3. Sync Gradle dependencies.
4. Connect an Android device with USB debugging enabled.
5. Click **Run 'app'**.

---

## 🔒 Security & Privacy Guardrails

- **Zero Data Harvesting**: Screen inspection runs locally on-device.
- **Financial & Destructive Intercepts**: Any payment action or account deletion is blocked until the user explicitly taps "Authorize".
- **Instant Kill-Switch**: The floating HUD includes an emergency stop button that terminates autonomous actions immediately.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
