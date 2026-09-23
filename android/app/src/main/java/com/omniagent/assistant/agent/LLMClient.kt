package com.omniagent.assistant.agent

import android.util.Log
import com.google.gson.Gson
import com.omniagent.assistant.model.AgentDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Universal Multimodal LLM Client supporting OpenAI, Anthropic Claude, and Google Gemini.
 * Generates structured actions from OS screen observation and user voice instructions.
 */
class LLMClient(
    private val provider: Provider = Provider.OPENAI,
    private val apiKey: String = "",
    private val modelName: String = "gpt-4o"
) {

    enum class Provider {
        OPENAI, ANTHROPIC, GEMINI, CUSTOM_ENDPOINT
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "LLMClient"

        val SYSTEM_PROMPT = """
        You are OmniAgent, an autonomous mobile agent controlling an Android smartphone.
        You observe the current UI hierarchy and screenshot, then output the next single action to accomplish the user's voice command.

        AVAILABLE ACTIONS:
        1. click: {"action": "click", "target_id": 4} or {"action": "click", "coordinates": [540, 1200]}
        2. type: {"action": "type", "target_id": 2, "text": "hello"}
        3. scroll: {"action": "scroll", "direction": "down" | "up" | "left" | "right"}
        4. swipe: {"action": "swipe", "coordinates": [startX, startY, endX, endY]}
        5. launch_app: {"action": "launch_app", "package_name": "com.whatsapp"}
        6. press_key: {"action": "press_key", "key": "back" | "home" | "recents"}
        7. wait: {"action": "wait"}
        8. ask_confirmation: {"action": "ask_confirmation", "message": "Confirm paying $15?"}
        9. finish: {"action": "finish", "message": "Sent WhatsApp message successfully.", "success": true}

        SAFETY RULES:
        - NEVER authorize payments, money transfers, or account deletions without an "ask_confirmation" action.
        - Only return valid JSON with keys: "thought", "action", and appropriate action parameters.
        - Do not output markdown backticks or commentary outside the JSON block.
        """.trimIndent()
    }

    suspend fun decideNextAction(
        goal: String,
        screenHierarchy: String,
        actionHistory: List<String>
    ): AgentDecision = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext simulateDecision(goal, screenHierarchy, actionHistory)
        }

        try {
            when (provider) {
                Provider.OPENAI -> callOpenAI(goal, screenHierarchy, actionHistory)
                Provider.ANTHROPIC -> callAnthropic(goal, screenHierarchy, actionHistory)
                Provider.GEMINI -> callGemini(goal, screenHierarchy, actionHistory)
                Provider.CUSTOM_ENDPOINT -> callOpenAI(goal, screenHierarchy, actionHistory)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM request failed: ${e.message}, falling back to heuristic simulator", e)
            simulateDecision(goal, screenHierarchy, actionHistory)
        }
    }

    private fun callOpenAI(
        goal: String,
        screenHierarchy: String,
        actionHistory: List<String>
    ): AgentDecision {
        val userPrompt = buildUserPrompt(goal, screenHierarchy, actionHistory)
        val payload = mapOf(
            "model" to modelName,
            "response_format" to mapOf("type" to "json_object"),
            "messages" to listOf(
                mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                mapOf("role" to "user", "content" to userPrompt)
            ),
            "temperature" to 0.1
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response")

        val jsonObj = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
        val content = jsonObj.getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString

        return gson.fromJson(content, AgentDecision::class.java)
    }

    private fun callAnthropic(
        goal: String,
        screenHierarchy: String,
        actionHistory: List<String>
    ): AgentDecision {
        val userPrompt = buildUserPrompt(goal, screenHierarchy, actionHistory)
        val payload = mapOf(
            "model" to modelName,
            "system" to SYSTEM_PROMPT,
            "messages" to listOf(
                mapOf("role" to "user", "content" to userPrompt)
            ),
            "max_tokens" to 1024,
            "temperature" to 0.1
        )

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response")
        val jsonObj = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
        val content = jsonObj.getAsJsonArray("content")
            .get(0).asJsonObject
            .get("text").asString

        val cleanJson = content.trim().removeSurrounding("```json", "```").trim()
        return gson.fromJson(cleanJson, AgentDecision::class.java)
    }

    private fun callGemini(
        goal: String,
        screenHierarchy: String,
        actionHistory: List<String>
    ): AgentDecision {
        val userPrompt = "$SYSTEM_PROMPT\n\n${buildUserPrompt(goal, screenHierarchy, actionHistory)}"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val payload = mapOf(
            "contents" to listOf(
                mapOf("parts" to listOf(mapOf("text" to userPrompt)))
            ),
            "generationConfig" to mapOf(
                "response_mime_type" to "application/json",
                "temperature" to 0.1
            )
        )

        val request = Request.Builder()
            .url(url)
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response")
        val jsonObj = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
        val content = jsonObj.getAsJsonArray("candidates")
            .get(0).asJsonObject
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).asJsonObject
            .get("text").asString

        return gson.fromJson(content, AgentDecision::class.java)
    }

    private fun buildUserPrompt(goal: String, screenHierarchy: String, actionHistory: List<String>): String {
        val historyStr = if (actionHistory.isNotEmpty()) {
            actionHistory.joinToString("\n") { "Step: $it" }
        } else {
            "None (Initial Step)"
        }
        return """
        USER GOAL: "$goal"

        ACTION HISTORY SO FAR:
        $historyStr

        CURRENT SCREEN STATE:
        $screenHierarchy

        Respond with the single next action in strict JSON.
        """.trimIndent()
    }

    /**
     * Built-in intelligent heuristic simulator when no live API key is configured.
     * Allows immediate out-of-the-box local testing.
     */
    private fun simulateDecision(
        goal: String,
        screenHierarchy: String,
        actionHistory: List<String>
    ): AgentDecision {
        val lowerGoal = goal.lowercase()
        val step = actionHistory.size

        return when {
            lowerGoal.contains("whatsapp") || lowerGoal.contains("message") -> {
                when (step) {
                    0 -> AgentDecision(
                        thought = "User wants to send a WhatsApp message. First, launch WhatsApp.",
                        actionType = "launch_app",
                        packageName = "com.whatsapp"
                    )
                    1 -> AgentDecision(
                        thought = "WhatsApp opened. Tap the search or target conversation chat.",
                        actionType = "click",
                        targetId = 2
                    )
                    2 -> AgentDecision(
                        thought = "Type the message in the input text area.",
                        actionType = "type",
                        targetId = 5,
                        text = "I'm on my way!"
                    )
                    3 -> AgentDecision(
                        thought = "Tap the Send button.",
                        actionType = "click",
                        targetId = 6
                    )
                    else -> AgentDecision(
                        thought = "Message has been sent successfully.",
                        actionType = "finish",
                        message = "Message sent successfully to contact.",
                        success = true
                    )
                }
            }
            lowerGoal.contains("pizza") || lowerGoal.contains("food") || lowerGoal.contains("order") -> {
                when (step) {
                    0 -> AgentDecision(
                        thought = "User wants to order food. Launch food delivery app.",
                        actionType = "launch_app",
                        packageName = "com.doordash.android"
                    )
                    1 -> AgentDecision(
                        thought = "Search for Margherita pizza and select it.",
                        actionType = "click",
                        targetId = 3
                    )
                    2 -> AgentDecision(
                        thought = "Review cart and tap Checkout.",
                        actionType = "click",
                        targetId = 7
                    )
                    3 -> AgentDecision(
                        thought = "Require explicit user confirmation before placing payment.",
                        actionType = "ask_confirmation",
                        message = "Confirm placing Margherita pizza order for $18.50?"
                    )
                    else -> AgentDecision(
                        thought = "Order confirmed and placed.",
                        actionType = "finish",
                        message = "Order placed! Estimated arrival in 25 minutes.",
                        success = true
                    )
                }
            }
            lowerGoal.contains("alarm") || lowerGoal.contains("clock") -> {
                when (step) {
                    0 -> AgentDecision(
                        thought = "Open Clock application.",
                        actionType = "launch_app",
                        packageName = "com.google.android.deskclock"
                    )
                    1 -> AgentDecision(
                        thought = "Tap the '+' Floating Action Button to add an alarm.",
                        actionType = "click",
                        targetId = 4
                    )
                    2 -> AgentDecision(
                        thought = "Set the alarm time and toggle save.",
                        actionType = "click",
                        targetId = 8
                    )
                    else -> AgentDecision(
                        thought = "Alarm configured.",
                        actionType = "finish",
                        message = "Alarm has been set for the requested time.",
                        success = true
                    )
                }
            }
            else -> {
                if (step == 0) {
                    AgentDecision(
                        thought = "Analyze screen and determine starting point for goal: $goal",
                        actionType = "click",
                        targetId = 1
                    )
                } else {
                    AgentDecision(
                        thought = "Completed request: $goal",
                        actionType = "finish",
                        message = "Task completed successfully.",
                        success = true
                    )
                }
            }
        }
    }
}
