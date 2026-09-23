# OmniAgent AI: Autonomous Android OS Agentic Assistant

> **OmniAgent** is an end-to-end, system-level autonomous agent for Android that executes complex, multi-hop tasks across any application using simple natural voice commands.

---

## 1. Executive Summary & Capabilities

OmniAgent does not rely on fragile per-app API integrations. Instead, it operates like a human user by:
1. **Listening continuously** for voice commands via a foreground service with continuous speech recognition (`SpeechRecognizer`).
2. **Inspecting the on-screen UI** using Android's native `AccessibilityService` (`AccessibilityNodeInfo` tree traversal and bounds detection).
3. **Reasoning over the UI state** using Multimodal Vision-Language-Action (VLA) models (GPT-4o, Claude 3.5 Sonnet, Gemini 2.0 Flash) through structured JSON action schemas.
4. **Executing synthetic actions** directly on the operating system (`dispatchGesture` for coordinate taps, swipes, and pinches; `ACTION_SET_TEXT` for typing; `GLOBAL_ACTION_BACK/HOME` for navigation).
5. **Enforcing safety guardrails** with explicit user confirmation for sensitive operations (such as making payments, purchasing food, booking rides, or modifying critical device settings).
6. **Giving real-time visual and voice feedback** via a floating dynamic HUD (`SYSTEM_ALERT_WINDOW`) and Android Text-to-Speech (`TextToSpeech`).

---

## 2. System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           OmniAgent Android                             │
├───────────────────────────────┬─────────────────────────────────────────┤
│          INPUT LAYER          │            ACTION & HUD LAYER           │
│  • VoiceAssistantService.kt   │  • FloatingOverlayService.kt            │
│    - SpeechRecognizer         │    - WindowManager Dynamic Island HUD   │
│    - Hotword & Wake Detection │    - Real-time reasoning pill           │
│    - TextToSpeech (TTS)       │    - Emergency Stop Kill-switch         │
├───────────────────────────────┴─────────────────────────────────────────┤
│                           REASONING CORE                                │
│  • AgentOrchestrator.kt                                                 │
│    - ReAct Loop: Observe -> Reason -> Act -> Verify                     │
│    - Safety Confirmation Interceptor (Payments / Sensitive Data)        │
│    - Max-step guardrails (prevents infinite loops)                      │
│                                                                         │
│  • LLMClient.kt                                                         │
│    - Structured JSON Action Dispatcher                                  │
│    - Providers: OpenAI GPT-4o, Anthropic Claude 3.5, Gemini 2.0 Flash   │
│    - Built-in heuristic offline simulator fallback                      │
├─────────────────────────────────────────────────────────────────────────┤
│                          OS AUTOMATION ENGINE                           │
│  • AgentAccessibilityService.kt                                         │
│    - Tree Traverser & UIHierarchyParser                                │
│    - Coordinate Tap Generator (dispatchGesture + Path)                  │
│    - Synthetic Swipe / Scroll / Drag Engine                             │
│    - IME Text Injector (ACTION_SET_TEXT & ACTION_FOCUS)                 │
│    - Global Navigation (Home, Back, Recents, Notifications)             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Project File Structure

The complete native Android project is structured as follows:

```
OmniAgent-Android/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── setup-and-grant-permissions.sh
├── README.md
└── app/
    ├── build.gradle.kts
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── res/
            │   ├── values/
            │   │   ├── strings.xml
            │   │   └── themes.xml
            │   └── xml/
            │       └── accessibility_service_config.xml
            └── java/com/omniagent/assistant/
                ├── model/
                │   ├── UIElement.kt             // Parsed UI node representation
                │   └── AgentAction.kt           // Sealed classes & Decision models
                ├── service/
                │   ├── AgentAccessibilityService.kt // OS Touch & Gesture Engine
                │   ├── VoiceAssistantService.kt     // Continuous Voice Listening & TTS
                │   └── FloatingOverlayService.kt    // Draggable Floating Dynamic Island
                ├── agent/
                │   ├── UIHierarchyParser.kt     // Compresses node tree for LLM
                │   ├── LLMClient.kt             // OpenAI / Claude / Gemini API caller
                │   └── AgentOrchestrator.kt     // The ReAct decision-action loop
                ├── util/
                │   └── PermissionHelper.kt      // Checks & launches permission intents
                └── ui/
                    └── MainActivity.kt          // Material 3 Compose setup & dashboard
```

---

## 4. How Key Components Work

### 4.1 Accessibility Service (`AgentAccessibilityService.kt`)
The accessibility service is configured in `accessibility_service_config.xml` with:
- `canRetrieveWindowContent="true"`: Accesses the full visual widget tree.
- `canPerformGestures="true"`: Allows programmatic touch strokes.
- `accessibilityFlags="flagRetrieveInteractiveWindows|flagReportViewIds|flagIncludeNotImportantViews"`: Ensures custom views, web views, and dialogs are captured.

When clicking an element:
1. It first attempts native `node.performAction(AccessibilityNodeInfo.ACTION_CLICK)`.
2. If the node is not natively clickable (e.g. custom layout), it calculates the screen centroid `(bounds.centerX, bounds.centerY)` and dispatches a synthetic stroke via `dispatchGesture()`.

### 4.2 UI Hierarchy Parser (`UIHierarchyParser.kt`)
Passing raw XML or thousands of view nodes exhausts LLM context windows and increases latency. The parser:
- Filters out non-visible and zero-dimension containers.
- Assigns short integer IDs (`[1]`, `[2]`, `[3]`).
- Extracts labels from `text`, `contentDescription`, `hintText`, or resource IDs.
- Outputs compact representations like:
  ```text
  [2] EditText hint="Search destinations" bounds=(40, 180, 320, 230) [clickable, editable]
  [5] Button "Confirm Ride" bounds=(40, 660, 320, 710) [clickable]
  ```

### 4.3 Structured Action Decision (`LLMClient.kt`)
The system prompt enforces a strict JSON schema:
```json
{
  "thought": "I will search for Luigi's Pizzeria and select the Margherita pizza.",
  "action": "click",
  "target_id": 3
}
```
Supported actions: `click`, `type`, `scroll`, `swipe`, `launch_app`, `press_key`, `wait`, `ask_confirmation`, and `finish`.

### 4.4 Sensitive Action Safety Interceptor
Autonomous agents must not inadvertently spend money, order items, or delete data without permission. When the LLM detects an irreversible or financial action, it issues an `ask_confirmation` action:
- The agent halts execution.
- The floating HUD highlights the question in amber with audio prompt.
- The user must explicitly approve or decline the action.

---

## 5. One-Step Device Permission Setup (via ADB)

To bypass manual Android permission dialogs during development, execute:

```bash
# 1. Grant Voice Audio Permission
adb shell pm grant com.omniagent.assistant android.permission.RECORD_AUDIO

# 2. Grant Floating HUD Permission
adb shell appops set com.omniagent.assistant SYSTEM_ALERT_WINDOW allow

# 3. Enable Accessibility Service Engine
adb shell settings put secure enabled_accessibility_services com.omniagent.assistant/com.omniagent.assistant.service.AgentAccessibilityService
adb shell settings put secure accessibility_enabled 1

# 4. Disable Battery Optimization (Doze Mode Bypass)
adb shell dumpsys deviceidle whitelist +com.omniagent.assistant

# 5. Launch OmniAgent
adb shell am start -n com.omniagent.assistant/.ui.MainActivity
```

---

## 6. Interactive Web Simulator & Studio

A live simulation environment is running on **port 3000**:
- **Virtual Android Phone**: Test voice actions across WhatsApp, DoorDash, Uber, Alarms, and Settings.
- **Voice Waveform & Mic Input**: Real-time browser speech recognition (`webkitSpeechRecognition`) + preconfigured test commands.
- **Visual Touch Ripple**: Shows the synthetic tap coordinates and typing animation on the phone screen.
- **Accessibility Tree Inspector**: Real-time view of `AccessibilityNodeInfo` elements and prompt tokenization.
- **One-Click Code Exporter**: Download the entire project as `OmniAgent-Android.zip`.
