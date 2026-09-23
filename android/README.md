# OmniAgent Android - Autonomous Mobile OS Voice Agent

OmniAgent is a system-level agentic assistant for Android that carries out end-to-end actions across any application through natural voice commands.

## Architecture Highlights
- **Engine**: Android `AccessibilityService` (`AgentAccessibilityService.kt`) with node-tree parsing, coordinate gesture generation (`dispatchGesture`), text insertion, and global keys.
- **Voice Loop**: Foreground `VoiceAssistantService.kt` with continuous `SpeechRecognizer` + `TextToSpeech` feedback.
- **Floating HUD**: Draggable `FloatingOverlayService.kt` with live agent thought pills and immediate emergency cancel button.
- **Brain**: `AgentOrchestrator.kt` ReAct loop with `LLMClient.kt` supporting GPT-4o, Claude 3.5 Sonnet, and Gemini 2.0 Flash with safety guardrails on sensitive actions (payments, account deletion).

## Quick ADB Setup
```bash
./setup-and-grant-permissions.sh
```

## How to Build in Android Studio
1. Open Android Studio Jellyfish / Koala (or newer).
2. Open `/home/user/OmniAgent-Android`.
3. Sync Gradle and run on device or emulator (Android 8.0+ API 26-34).
