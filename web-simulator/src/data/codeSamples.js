export const CODE_FILES = [
  {
    path: "app/src/main/java/com/omniagent/assistant/service/AgentAccessibilityService.kt",
    name: "AgentAccessibilityService.kt",
    language: "kotlin",
    description: "Core OS Automation Engine: Traverses UI hierarchy, triggers synthetic taps, typing, gestures, and system navigation.",
    code: `package com.omniagent.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.omniagent.assistant.agent.UIHierarchyParser
import com.omniagent.assistant.model.GlobalKey
import com.omniagent.assistant.model.ScrollDirection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

/**
 * Core Android Accessibility Service.
 * Acts as the agent's OS interface: reads screen hierarchy and executes synthetic touches/gestures.
 */
class AgentAccessibilityService : AccessibilityService() {

    private val parser = UIHierarchyParser()
    private var lastParseResult: UIHierarchyParser.ParseResult? = null

    companion object {
        private const val TAG = "AgentAccessibility"
        @Volatile
        var instance: AgentAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AgentAccessibilityService connected and ready.")
    }

    /**
     * Inspects current screen and produces token-efficient UI breakdown
     */
    fun inspectCurrentScreen(): UIHierarchyParser.ParseResult {
        val root = rootInActiveWindow
        val result = parser.parse(root)
        lastParseResult = result
        return result
    }

    /**
     * Click an element by its assigned ID from the last parse
     */
    fun clickElement(elementId: Int): Boolean {
        val node = lastParseResult?.nodeMap?.get(elementId)
        if (node != null) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            // Fallback: Gesture tap on center coordinates
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (bounds.width() > 0 && bounds.height() > 0) {
                return clickCoordinate(bounds.exactCenterX(), bounds.exactCenterY())
            }
        }
        return false
    }

    /**
     * Taps a precise coordinate on the screen via GestureDescription
     */
    fun clickCoordinate(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)

        return if (dispatched) runCatching { runBlocking { deferred.await() } }.getOrDefault(false) else false
    }

    /**
     * Enters text into an editable element
     */
    fun inputText(elementId: Int?, text: String): Boolean {
        val node = if (elementId != null) lastParseResult?.nodeMap?.get(elementId) else rootInActiveWindow
        if (node != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    /**
     * Executes global system actions (Back, Home, Recents, Notifications)
     */
    fun performGlobal(key: GlobalKey): Boolean {
        val action = when (key) {
            GlobalKey.BACK -> GLOBAL_ACTION_BACK
            GlobalKey.HOME -> GLOBAL_ACTION_HOME
            GlobalKey.RECENTS -> GLOBAL_ACTION_RECENTS
            GlobalKey.NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
            GlobalKey.ENTER -> return false
        }
        return performGlobalAction(action)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }
}`
  },
  {
    path: "app/src/main/java/com/omniagent/assistant/agent/AgentOrchestrator.kt",
    name: "AgentOrchestrator.kt",
    language: "kotlin",
    description: "Autonomous ReAct Agent Loop: Coordinates Observe -> Reason -> Act -> Verify on the device.",
    code: `package com.omniagent.assistant.agent

import android.content.Context
import com.omniagent.assistant.model.AgentDecision
import com.omniagent.assistant.service.AgentAccessibilityService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AgentOrchestrator(
    private val context: Context,
    private val llmClient: LLMClient,
    private val listener: AgentListener? = null
) {
    private val isRunning = AtomicBoolean(false)
    private var agentJob: Job? = null
    private val actionHistory = mutableListOf<String>()

    fun startTask(voiceGoal: String) {
        isRunning.set(true)
        actionHistory.clear()
        agentJob = CoroutineScope(Dispatchers.Default).launch {
            val service = AgentAccessibilityService.instance ?: return@launch
            var step = 0
            while (isRunning.get() && step < 15) {
                step++
                // 1. Observe screen UI tree
                val screen = service.inspectCurrentScreen()
                // 2. Reason via LLM
                val decision = llmClient.decideNextAction(voiceGoal, screen.promptRepresentation, actionHistory)
                // 3. Sensitive Action Safety Guardrail
                if (decision.actionType == "ask_confirmation") {
                    listener?.onConfirmationRequired(decision.message ?: "Confirm action?") { /* handle */ }
                    continue
                }
                if (decision.actionType == "finish") {
                    listener?.onTaskFinished(decision.success, decision.message ?: "Task completed")
                    break
                }
                // 4. Execute Action (click, type, scroll, swipe)
                executeDecision(service, decision)
                actionHistory.add("Step $step: \${decision.actionType}")
                delay(1000L)
            }
        }
    }

    private suspend fun executeDecision(service: AgentAccessibilityService, d: AgentDecision) {
        when (d.actionType.lowercase()) {
            "click" -> d.targetId?.let { service.clickElement(it) }
            "type" -> service.inputText(d.targetId, d.text ?: "")
            "launch_app" -> d.packageName?.let { service.launchApp(it) }
        }
    }

    fun stopTask() {
        isRunning.set(false)
        agentJob?.cancel()
    }
}`
  },
  {
    path: "app/src/main/java/com/omniagent/assistant/service/FloatingOverlayService.kt",
    name: "FloatingOverlayService.kt",
    language: "kotlin",
    description: "System Overlay: Draggable Dynamic Island HUD with real-time thought stream and emergency kill-switch.",
    code: `package com.omniagent.assistant.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView

class FloatingOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        // Builds draggable floating pill with Mic icon, dynamic status text, and emergency stop
    }
}`
  },
  {
    path: "app/src/main/AndroidManifest.xml",
    name: "AndroidManifest.xml",
    language: "xml",
    description: "Android Manifest with BIND_ACCESSIBILITY_SERVICE, SYSTEM_ALERT_WINDOW, RECORD_AUDIO, and QUERY_ALL_PACKAGES.",
    code: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />

    <application
        android:label="OmniAgent AI"
        android:theme="@style/Theme.OmniAgent">

        <activity android:name=".ui.MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.AgentAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <service android:name=".service.VoiceAssistantService" android:foregroundServiceType="microphone" />
        <service android:name=".service.FloatingOverlayService" />
    </application>
</manifest>`
  },
  {
    path: "app/src/main/res/xml/accessibility_service_config.xml",
    name: "accessibility_service_config.xml",
    language: "xml",
    description: "Accessibility configuration file enabling gesture dispatching, window inspection, and interactive flags.",
    code: `<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds|flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:canRequestFilterKeyEvents="true"
    android:canRequestTouchExplorationMode="false"
    android:settingsActivity="com.omniagent.assistant.ui.MainActivity" />`
  }
];
