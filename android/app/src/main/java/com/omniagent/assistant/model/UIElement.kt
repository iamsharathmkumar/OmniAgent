package com.omniagent.assistant.model

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Normalized UI Element extracted from Android's AccessibilityNodeInfo tree.
 * Token-efficient representation passed to LLM for autonomous navigation.
 */
data class UIElement(
    val id: Int,
    val viewId: String?,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val hintText: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val packageName: String?,
    @Transient val nativeNode: AccessibilityNodeInfo? = null
) {
    val displayLabel: String
        get() = when {
            !text.isNullOrBlank() -> "\"$text\""
            !contentDescription.isNullOrBlank() -> "desc=\"$contentDescription\""
            !hintText.isNullOrBlank() -> "hint=\"$hintText\""
            !viewId.isNullOrBlank() -> "id=${viewId.substringAfterLast('/')}"
            else -> className.substringAfterLast('.')
        }

    fun toPromptString(): String {
        val traits = mutableListOf<String>()
        if (isClickable) traits.add("clickable")
        if (isEditable) traits.add("editable")
        if (isScrollable) traits.add("scrollable")
        if (isCheckable) traits.add(if (isChecked) "checked" else "unchecked")
        val traitsStr = if (traits.isNotEmpty()) " [${traits.joinToString(", ")}]" else ""
        return "[$id] ${className.substringAfterLast('.')} $displayLabel bounds=(${bounds.left},${bounds.top},${bounds.right},${bounds.bottom})$traitsStr"
    }
}
