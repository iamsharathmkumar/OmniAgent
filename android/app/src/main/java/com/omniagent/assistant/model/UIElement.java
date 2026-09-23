package com.omniagent.assistant.model;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

public class UIElement {
    public final int id;
    public final String viewId;
    public final String className;
    public final String text;
    public final String contentDescription;
    public final Rect bounds;
    public final boolean isClickable;
    public final boolean isEditable;
    public final boolean isScrollable;
    public final AccessibilityNodeInfo nativeNode;

    public UIElement(int id, String viewId, String className, String text, String contentDescription,
                     Rect bounds, boolean isClickable, boolean isEditable, boolean isScrollable,
                     AccessibilityNodeInfo nativeNode) {
        this.id = id;
        this.viewId = viewId;
        this.className = className;
        this.text = text;
        this.contentDescription = contentDescription;
        this.bounds = bounds;
        this.isClickable = isClickable;
        this.isEditable = isEditable;
        this.isScrollable = isScrollable;
        this.nativeNode = nativeNode;
    }

    public String toPromptString() {
        String label = (text != null && !text.trim().isEmpty()) ? "\"" + text.trim() + "\"" :
                (contentDescription != null && !contentDescription.trim().isEmpty()) ? "desc=\"" + contentDescription.trim() + "\"" :
                        className != null ? className.substring(className.lastIndexOf('.') + 1) : "View";

        StringBuilder traits = new StringBuilder();
        if (isClickable) traits.append("clickable ");
        if (isEditable) traits.append("editable ");
        if (isScrollable) traits.append("scrollable ");

        return "[" + id + "] " + label + " bounds=(" + bounds.left + "," + bounds.top + "," + bounds.right + "," + bounds.bottom + ") [" + traits.toString().trim() + "]";
    }
}
