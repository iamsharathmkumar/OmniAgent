package com.omniagent.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.omniagent.assistant.agent.AgentOrchestrator
import com.omniagent.assistant.agent.LLMClient
import com.omniagent.assistant.model.AgentDecision
import com.omniagent.assistant.ui.MainActivity
import java.util.Locale

/**
 * Foreground service that listens for voice commands,
 * coordinates with AgentOrchestrator to control the phone,
 * and speaks responses back to the user via TextToSpeech.
 */
class VoiceAssistantService : Service(), TextToSpeech.OnInitListener, AgentOrchestrator.AgentListener {

    companion object {
        private const val TAG = "VoiceAssistant"
        private const val CHANNEL_ID = "omni_agent_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_LISTENING = "com.omniagent.START_LISTENING"
        const val ACTION_STOP_AGENT = "com.omniagent.STOP_AGENT"
        const val ACTION_EXECUTE_TEXT = "com.omniagent.EXECUTE_TEXT"
        const val EXTRA_TEXT_COMMAND = "extra_text_command"

        @Volatile
        var isRunning = false
            private set
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var orchestrator: AgentOrchestrator? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("OmniAgent is standing by for commands"))

        // Initialize Text to Speech
        tts = TextToSpeech(this, this)

        // Initialize default LLM client and Orchestrator
        val prefs = getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        val providerStr = prefs.getString("provider", "OPENAI") ?: "OPENAI"
        val model = prefs.getString("model", "gpt-4o") ?: "gpt-4o"

        val provider = try {
            LLMClient.Provider.valueOf(providerStr)
        } catch (e: Exception) {
            LLMClient.Provider.OPENAI
        }

        val client = LLMClient(provider, apiKey, model)
        orchestrator = AgentOrchestrator(this, client, this)

        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_STOP_AGENT -> orchestrator?.stopTask()
            ACTION_EXECUTE_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT_COMMAND)
                if (!text.isNullOrBlank()) {
                    executeCommand(text)
                }
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ttsReady = true
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "SpeechRecognizer ready")
                    FloatingOverlayService.updateStatus(this@VoiceAssistantService, "Listening...")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    FloatingOverlayService.updateStatus(this@VoiceAssistantService, "Processing audio...")
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "Speech recognition error: $error")
                    FloatingOverlayService.updateStatus(this@VoiceAssistantService, "Tap mic to speak")
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val command = matches?.firstOrNull()
                    if (!command.isNullOrBlank()) {
                        Log.i(TAG, "Recognized command: $command")
                        executeCommand(command)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun executeCommand(command: String) {
        updateNotification("Executing: $command")
        FloatingOverlayService.updateStatus(this, "Goal: $command")
        speak("Working on it.")
        orchestrator?.startTask(command)
    }

    fun speak(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "omni_tts_utterance")
        }
    }

    // Orchestrator Callbacks
    override fun onStateChanged(state: AgentOrchestrator.AgentState, detail: String) {
        FloatingOverlayService.updateStatus(this, detail)
        updateNotification(detail)
    }

    override fun onActionExecuted(stepIndex: Int, decision: AgentDecision) {
        Log.d(TAG, "Step $stepIndex: ${decision.actionType}")
    }

    override fun onConfirmationRequired(question: String, onConfirm: (Boolean) -> Unit) {
        speak("Confirmation required: $question")
        FloatingOverlayService.showConfirmationDialog(this, question, onConfirm)
    }

    override fun onTaskFinished(success: Boolean, summary: String) {
        speak(summary)
        FloatingOverlayService.updateStatus(this, if (success) "✓ $summary" else "✗ $summary")
        updateNotification(summary)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OmniAgent Background Listener",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OmniAgent AI Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        speechRecognizer?.destroy()
        tts?.shutdown()
        orchestrator?.stopTask()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
