package com.omniagent.assistant.agent

import android.content.Context
import android.util.Log
import com.omniagent.assistant.model.AgentDecision
import com.omniagent.assistant.model.GlobalKey
import com.omniagent.assistant.model.ScrollDirection
import com.omniagent.assistant.service.AgentAccessibilityService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Autonomous Agent Orchestrator.
 * Implements the Observe -> Reason -> Act -> Verify loop on Android.
 */
class AgentOrchestrator(
    private val context: Context,
    private val llmClient: LLMClient,
    private val listener: AgentListener? = null
) {

    interface AgentListener {
        fun onStateChanged(state: AgentState, detail: String = "")
        fun onActionExecuted(stepIndex: Int, decision: AgentDecision)
        fun onConfirmationRequired(question: String, onConfirm: (Boolean) -> Unit)
        fun onTaskFinished(success: Boolean, summary: String)
    }

    enum class AgentState {
        IDLE,
        THINKING,
        EXECUTING,
        AWAITING_CONFIRMATION,
        FINISHED,
        CANCELLED,
        ERROR
    }

    companion object {
        private const val TAG = "AgentOrchestrator"
        private const val MAX_STEPS = 15
    }

    private val isRunning = AtomicBoolean(false)
    private var agentJob: Job? = null
    private val actionHistory = mutableListOf<String>()

    val isActive: Boolean
        get() = isRunning.get()

    fun startTask(voiceGoal: String) {
        if (isRunning.getAndSet(true)) {
            Log.w(TAG, "Task already running, aborting previous.")
            stopTask()
            isRunning.set(true)
        }

        actionHistory.clear()
        agentJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                runAgentLoop(voiceGoal)
            } catch (e: CancellationException) {
                Log.i(TAG, "Agent task cancelled by user.")
                withContext(Dispatchers.Main) {
                    listener?.onStateChanged(AgentState.CANCELLED, "Cancelled by user")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in agent loop", e)
                withContext(Dispatchers.Main) {
                    listener?.onStateChanged(AgentState.ERROR, e.localizedMessage ?: "Unknown error")
                }
            } finally {
                isRunning.set(false)
            }
        }
    }

    fun stopTask() {
        isRunning.set(false)
        agentJob?.cancel()
        agentJob = null
    }

    private suspend fun runAgentLoop(goal: String) {
        val service = AgentAccessibilityService.instance
        if (service == null) {
            withContext(Dispatchers.Main) {
                listener?.onStateChanged(
                    AgentState.ERROR,
                    "Accessibility Service is not enabled. Please enable it in Settings."
                )
            }
            return
        }

        var step = 0
        while (isRunning.get() && step < MAX_STEPS) {
            step++
            withContext(Dispatchers.Main) {
                listener?.onStateChanged(AgentState.THINKING, "Step $step: Analyzing screen...")
            }

            // 1. Observe
            val screenState = service.inspectCurrentScreen()

            // 2. Reason
            val decision = llmClient.decideNextAction(
                goal = goal,
                screenHierarchy = screenState.promptRepresentation,
                actionHistory = actionHistory
            )

            Log.i(TAG, "Step $step Decision: thought=${decision.thought}, action=${decision.actionType}")

            // 3. Handle Sensitive Confirmation
            if (decision.actionType.equals("ask_confirmation", ignoreCase = true)) {
                withContext(Dispatchers.Main) {
                    listener?.onStateChanged(AgentState.AWAITING_CONFIRMATION, decision.message ?: "Confirmation required")
                }
                val confirmed = awaitUserConfirmation(decision.message ?: "Authorize action?")
                if (!confirmed) {
                    withContext(Dispatchers.Main) {
                        listener?.onTaskFinished(false, "User declined sensitive action.")
                    }
                    return
                }
                actionHistory.add("User confirmed: ${decision.message}")
                continue
            }

            // 4. Handle Finish
            if (decision.actionType.equals("finish", ignoreCase = true)) {
                withContext(Dispatchers.Main) {
                    listener?.onActionExecuted(step, decision)
                    listener?.onTaskFinished(decision.success, decision.message ?: "Task finished.")
                }
                return
            }

            // 5. Act
            withContext(Dispatchers.Main) {
                listener?.onStateChanged(AgentState.EXECUTING, decision.thought)
                listener?.onActionExecuted(step, decision)
            }

            val success = executeDecision(service, decision)
            val historyEntry = "Action: ${decision.actionType} (target=${decision.targetId ?: decision.coordinates}), result=${if (success) "success" else "failed"}"
            actionHistory.add(historyEntry)

            // 6. Settle delay
            delay(1000L)
        }

        if (step >= MAX_STEPS) {
            withContext(Dispatchers.Main) {
                listener?.onTaskFinished(false, "Reached maximum step limit ($MAX_STEPS).")
            }
        }
    }

    private suspend fun executeDecision(
        service: AgentAccessibilityService,
        decision: AgentDecision
    ): Boolean {
        return when (decision.actionType.lowercase()) {
            "click" -> {
                when {
                    decision.targetId != null -> service.clickElement(decision.targetId)
                    decision.coordinates != null && decision.coordinates.size >= 2 ->
                        service.clickCoordinate(decision.coordinates[0], decision.coordinates[1])
                    else -> false
                }
            }
            "type" -> {
                val text = decision.text ?: ""
                service.inputText(decision.targetId, text)
            }
            "scroll" -> {
                val dir = when (decision.direction?.lowercase()) {
                    "up" -> ScrollDirection.UP
                    "left" -> ScrollDirection.LEFT
                    "right" -> ScrollDirection.RIGHT
                    else -> ScrollDirection.DOWN
                }
                service.scroll(dir, decision.targetId)
            }
            "swipe" -> {
                val coords = decision.coordinates ?: listOf(500f, 1500f, 500f, 500f)
                if (coords.size >= 4) {
                    service.swipe(coords[0], coords[1], coords[2], coords[3])
                } else {
                    false
                }
            }
            "launch_app" -> {
                decision.packageName?.let { service.launchApp(it) } ?: false
            }
            "press_key" -> {
                val key = when (decision.key?.lowercase()) {
                    "back" -> GlobalKey.BACK
                    "home" -> GlobalKey.HOME
                    "recents" -> GlobalKey.RECENTS
                    "notifications" -> GlobalKey.NOTIFICATIONS
                    else -> GlobalKey.BACK
                }
                service.performGlobal(key)
            }
            "wait" -> {
                delay(1200L)
                true
            }
            else -> {
                Log.w(TAG, "Unknown action: ${decision.actionType}")
                false
            }
        }
    }

    private suspend fun awaitUserConfirmation(prompt: String): Boolean = suspendCancellableCoroutine { cont ->
        listener?.onConfirmationRequired(prompt) { approved ->
            if (cont.isActive) {
                cont.resume(approved) {}
            }
        }
    }
}
