package com.omniagent.assistant.agent;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import com.omniagent.assistant.model.UIElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UIHierarchyParser {

    public static class ParseResult {
        public final String promptText;
        public final List<UIElement> elements;
        public final Map<Integer, AccessibilityNodeInfo> nodeMap;
        public final String packageName;

        public ParseResult(String promptText, List<UIElement> elements,
                           Map<Integer, AccessibilityNodeInfo> nodeMap, String packageName) {
            this.promptText = promptText;
            this.elements = elements;
            this.nodeMap = nodeMap;
            this.packageName = packageName;
        }
    }

    public ParseResult parse(AccessibilityNodeInfo rootNode) {
        Map<Integer, AccessibilityNodeInfo> map = new HashMap<>();
        List<UIElement> list = new ArrayList<>();
        if (rootNode == null) {
            return new ParseResult("Empty or protected window", list, map, "unknown");
        }

        AtomicInteger counter = new AtomicInteger(1);
        String pkg = rootNode.getPackageName() != null ? rootNode.getPackageName().toString() : "unknown";
        traverse(rootNode, counter, list, map);

        StringBuilder sb = new StringBuilder();
        sb.append("Current App Package: ").append(pkg).append("\n");
        sb.append("Screen Interactive Elements:\n");
        for (UIElement el : list) {
            sb.append(el.toPromptString()).append("\n");
        }

        return new ParseResult(sb.toString(), list, map, pkg);
    }

    private void traverse(AccessibilityNodeInfo node, AtomicInteger counter,
                          List<UIElement> list, Map<Integer, AccessibilityNodeInfo> map) {
        if (node == null || !node.isVisibleToUser()) return;

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        String text = node.getText() != null ? node.getText().toString().trim() : null;
        String desc = node.getContentDescription() != null ? node.getContentDescription().toString().trim() : null;
        boolean clickable = node.isClickable();
        boolean editable = node.isEditable();
        boolean scrollable = node.isScrollable();

        if (clickable || editable || scrollable || (text != null && !text.isEmpty()) || (desc != null && !desc.isEmpty())) {
            int id = counter.getAndIncrement();
            String className = node.getClassName() != null ? node.getClassName().toString() : "View";
            String viewId = node.getViewIdResourceName();
            UIElement element = new UIElement(id, viewId, className, text, desc, bounds, clickable, editable, scrollable, node);
            list.add(element);
            map.put(id, node);
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            traverse(node.getChild(i), counter, list, map);
        }
    }
}
