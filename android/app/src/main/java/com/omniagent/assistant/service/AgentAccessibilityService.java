package com.omniagent.assistant.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.omniagent.assistant.agent.UIHierarchyParser;

public class AgentAccessibilityService extends AccessibilityService {

    private static final String TAG = "AgentAccessibility";
    public static volatile AgentAccessibilityService instance = null;

    private final UIHierarchyParser parser = new UIHierarchyParser();
    private UIHierarchyParser.ParseResult lastResult = null;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "AgentAccessibilityService connected.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {
        Log.w(TAG, "AgentAccessibilityService interrupted.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) instance = null;
        Log.i(TAG, "AgentAccessibilityService destroyed.");
    }

    public UIHierarchyParser.ParseResult inspectScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        lastResult = parser.parse(root);
        return lastResult;
    }

    public boolean clickElement(int elementId) {
        if (lastResult == null || lastResult.nodeMap == null) return false;
        AccessibilityNodeInfo node = lastResult.nodeMap.get(elementId);
        if (node != null) {
            if (node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable() && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true;
                }
                parent = parent.getParent();
            }
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            if (rect.width() > 0 && rect.height() > 0) {
                return clickCoordinates(rect.centerX(), rect.centerY());
            }
        }
        return false;
    }

    public boolean clickCoordinates(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 50);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        return dispatchGesture(gesture, null, null);
    }

    public boolean inputText(int elementId, String text) {
        AccessibilityNodeInfo node = (lastResult != null && lastResult.nodeMap != null) ?
                lastResult.nodeMap.get(elementId) : null;
        if (node == null) {
            node = findEditable(getRootInActiveWindow());
        }
        if (node != null && node.isEditable()) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        }
        return false;
    }

    public boolean scroll(boolean down) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo scrollable = findScrollable(root);
        if (scrollable != null) {
            int action = down ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
            return scrollable.performAction(action);
        }
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        Path path = new Path();
        if (down) {
            path.moveTo(w / 2f, h * 0.75f);
            path.lineTo(w / 2f, h * 0.25f);
        } else {
            path.moveTo(w / 2f, h * 0.25f);
            path.lineTo(w / 2f, h * 0.75f);
        }
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 300);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        return dispatchGesture(gesture, null, null);
    }

    public boolean launchApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch package: " + packageName, e);
        }
        return false;
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo f = findEditable(node.getChild(i));
            if (f != null) return f;
        }
        return null;
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo f = findScrollable(node.getChild(i));
            if (f != null) return f;
        }
        return null;
    }
}
