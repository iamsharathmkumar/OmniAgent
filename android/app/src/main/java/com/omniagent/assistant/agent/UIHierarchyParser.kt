package com.omniagent.assistant.agent

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.omniagent.assistant.model.UIElement
import java.util.concurrent.atomic.AtomicInteger

/**
 * Parses and compresses the Android AccessibilityNodeInfo tree into a clean,
 * token-efficient representation for Multimodal & LLM reasoning agents.
 */
class UIHierarchyParser {

    private val elementMap = mutableMapOf<Int, AccessibilityNodeInfo>()

    data class ParseResult(
        val promptRepresentation: String,
        val elements: List<UIElement>,
        val currentPackage: String?,
        val nodeMap: Map<Int, AccessibilityNodeInfo>
    )

    fun parse(rootNode: AccessibilityNodeInfo?): ParseResult {
        elementMap.clear()
        if (rootNode == null) {
            return ParseResult("Empty screen or secure surface (cannot inspect)", emptyList(), null, emptyMap())
        }

        val counter = AtomicInteger(1)
        val extractedElements = mutableListOf<UIElement>()
        val packageName = rootNode.packageName?.toString()

        traverse(rootNode, counter, extractedElements)

        val promptBuilder = StringBuilder()
        promptBuilder.appendLine("Current Package: ${packageName ?: "unknown"}")
        promptBuilder.appendLine("Screen Interactive Elements:")

        for (el in extractedElements) {
            promptBuilder.appendLine(el.toPromptString())
        }

        return ParseResult(
            promptRepresentation = promptBuilder.toString(),
            elements = extractedElements,
            currentPackage = packageName,
            nodeMap = HashMap(elementMap)
        )
    }

    private fun traverse(
        node: AccessibilityNodeInfo?,
        counter: AtomicInteger,
        accumulator: MutableList<UIElement>
    ) {
        if (node == null || !node.isVisibleToUser) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Filter out zero-size or off-screen invisible elements
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val hint = node.hintText?.toString()?.trim()
        val isClickable = node.isClickable
        val isEditable = node.isEditable
        val isScrollable = node.isScrollable
        val isCheckable = node.isCheckable

        val isInteractive = isClickable || isEditable || isScrollable || isCheckable
        val hasMeaningfulContent = !text.isNullOrBlank() || !desc.isNullOrBlank() || !hint.isNullOrBlank()

        if (isInteractive || hasMeaningfulContent) {
            val id = counter.getAndIncrement()
            val element = UIElement(
                id = id,
                viewId = node.viewIdResourceName,
                className = node.className?.toString() ?: "android.view.View",
                text = text,
                contentDescription = desc,
                hintText = hint,
                bounds = bounds,
                isClickable = isClickable,
                isEditable = isEditable,
                isScrollable = isScrollable,
                isCheckable = isCheckable,
                isChecked = node.isChecked,
                isEnabled = node.isEnabled,
                packageName = node.packageName?.toString(),
                nativeNode = node
            )
            accumulator.add(element)
            elementMap[id] = node
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            traverse(child, counter, accumulator)
        }
    }
}
