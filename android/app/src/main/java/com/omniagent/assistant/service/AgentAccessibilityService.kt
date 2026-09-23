package com.omniagent.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
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
import java.util.concurrent.atomic.AtomicBoolean

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can be used to trigger reactive actions on window content changes
    }

    override fun onInterrupt() {
        Log.w(TAG, "AgentAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "AgentAccessibilityService destroyed.")
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
            // Attempt native accessibility click first
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.d(TAG, "Clicked node id=$elementId via ACTION_CLICK")
                return true
            }
            // Fallback to parent clickable node if available
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.d(TAG, "Clicked parent of id=$elementId via ACTION_CLICK")
                    return true
                }
                parent = parent.parent
            }
            // Fallback: Gesture tap on center coordinates
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (bounds.width() > 0 && bounds.height() > 0) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                Log.d(TAG, "Fallback click via gesture tap at ($cx, $cy)")
                return clickCoordinate(cx, cy)
            }
        }
        Log.e(TAG, "Failed to click element id=$elementId")
        return false
    }

    /**
     * Taps a precise coordinate on the screen via GestureDescription
     */
    fun clickCoordinate(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
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

        return if (dispatched) {
            runCatching { runBlocking { deferred.await() } }.getOrDefault(false)
        } else {
            false
        }
    }

    /**
     * Enters text into an editable element
     */
    fun inputText(elementId: Int?, text: String, clearExisting: Boolean = false): Boolean {
        val node = if (elementId != null) {
            lastParseResult?.nodeMap?.get(elementId)
        } else {
            findFocusedOrFirstEditableNode(rootInActiveWindow)
        }

        if (node != null && node.isEditable) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "Input text into id=$elementId success=$success")
            return success
        }
        Log.e(TAG, "Could not find editable node for text entry: $text")
        return false
    }

    /**
     * Scroll up, down, left, or right
     */
    fun scroll(direction: ScrollDirection, elementId: Int? = null): Boolean {
        val node = if (elementId != null) {
            lastParseResult?.nodeMap?.get(elementId)
        } else {
            findScrollableNode(rootInActiveWindow)
        }

        if (node != null) {
            val action = when (direction) {
                ScrollDirection.DOWN, ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                ScrollDirection.UP, ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (node.performAction(action)) {
                return true
            }
        }

        // Gesture-based swipe fallback
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()

        return when (direction) {
            ScrollDirection.DOWN -> swipe(width / 2, height * 0.75f, width / 2, height * 0.25f, 350)
            ScrollDirection.UP -> swipe(width / 2, height * 0.25f, width / 2, height * 0.75f, 350)
            ScrollDirection.RIGHT -> swipe(width * 0.8f, height / 2, width * 0.2f, height / 2, 350)
            ScrollDirection.LEFT -> swipe(width * 0.2f, height / 2, width * 0.8f, height / 2, 350)
        }
    }

    /**
     * Synthetic swipe gesture between two points
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
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

        return if (dispatched) {
            runCatching { runBlocking { deferred.await() } }.getOrDefault(false)
        } else {
            false
        }
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
            GlobalKey.ENTER -> return false // Handled via IME / Key event
        }
        return performGlobalAction(action)
    }

    /**
     * Launches an application given its package name
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            } else {
                Log.w(TAG, "No launch intent found for $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            false
        }
    }

    private fun findFocusedOrFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && node.isEditable) return node
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = findFocusedOrFirstEditableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }
}
