package com.omniagent.assistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.omniagent.assistant.agent.AgentOrchestrator;
import com.omniagent.assistant.agent.HumanoidTaskEngine;
import com.omniagent.assistant.agent.LLMClient;
import com.omniagent.assistant.ui.MainActivity;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceAssistantService extends Service implements TextToSpeech.OnInitListener, AgentOrchestrator.OrchestratorListener {

    private static final String TAG = "VoiceAssistant";
    public static final String ACTION_START_LISTEN = "com.omniagent.ACTION_START_LISTEN";
    public static final String ACTION_STOP = "com.omniagent.ACTION_STOP";
    public static final String ACTION_RUN_TEXT = "com.omniagent.ACTION_RUN_TEXT";
    public static final String ACTION_ENABLE_ALWAYS_ON = "com.omniagent.ACTION_ENABLE_ALWAYS_ON";
    public static final String EXTRA_COMMAND = "extra_command";

    private static final String CHANNEL_ID = "skai_voice_channel";
    private static final int NOTIF_ID = 2001;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private AgentOrchestrator orchestrator;

    private boolean isAlwaysOn = true;
    private boolean isListening = false;
    private long lastInteractionTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static class VoiceListenerImpl implements RecognitionListener {
        private final VoiceAssistantService service;

        VoiceListenerImpl(VoiceAssistantService service) {
            this.service = service;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            service.isListening = true;
            FloatingOverlayService.updateStatus(service, "SKAI: Ear active (Say 'SKAI')...");
        }

        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            service.isListening = false;
        }

        @Override
        public void onError(int error) {
            service.isListening = false;
            service.scheduleRestartListening(400);
        }

        @Override
        public void onResults(Bundle results) {
            service.isListening = false;
            ArrayList<String> matches = results != null ? results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) : null;
            if (matches != null && !matches.isEmpty()) {
                String heard = matches.get(0);
                service.processVoiceUtterance(heard);
            } else {
                service.scheduleRestartListening(300);
            }
        }

        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    }

    private static class RestartListeningTask implements Runnable {
        private final VoiceAssistantService service;

        RestartListeningTask(VoiceAssistantService service) {
            this.service = service;
        }

        @Override
        public void run() {
            if (!service.isListening && (service.tts == null || !service.tts.isSpeaking())) {
                service.startListening();
            } else {
                service.scheduleRestartListening(800);
            }
        }
    }

    private static class SpeechResumeTask implements Runnable {
        private final VoiceAssistantService service;

        SpeechResumeTask(VoiceAssistantService service) {
            this.service = service;
        }

        @Override
        public void run() {
            service.scheduleRestartListening(1000);
        }
    }

    private static class HumanoidTaskCallbackImpl implements HumanoidTaskEngine.TaskCallback {
        private final VoiceAssistantService service;

        HumanoidTaskCallbackImpl(VoiceAssistantService service) {
            this.service = service;
        }

        @Override
        public void onProgress(String message, String speech) {
            FloatingOverlayService.updateStatus(service, "SKAI: " + message);
            MainActivity.appendLog(message);
            service.speak(speech);
        }

        @Override
        public void onCompleted(boolean success, String summary, String speech) {
            FloatingOverlayService.updateStatus(service, (success ? "✓ " : "✗ ") + summary);
            MainActivity.appendLog(summary);
            service.speak(speech);
            service.scheduleRestartListening(3000);
        }
    }

    private final RestartListeningTask restartTask = new RestartListeningTask(this);

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("SKAI Humanoid Voice Assistant is Active (Hands-Free)"));

        tts = new TextToSpeech(this, this);

        LLMClient llmClient = LLMClient.fromPreferences(this);
        orchestrator = new AgentOrchestrator(this, llmClient, this);

        initSpeechRecognizer();
        scheduleRestartListening(1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String act = intent.getAction();
            if (ACTION_START_LISTEN.equals(act)) {
                lastInteractionTime = System.currentTimeMillis();
                startListening();
            } else if (ACTION_STOP.equals(act)) {
                if (orchestrator != null) orchestrator.stop();
                stopListening();
            } else if (ACTION_RUN_TEXT.equals(act)) {
                String cmd = intent.getStringExtra(EXTRA_COMMAND);
                if (cmd != null && !cmd.trim().isEmpty()) {
                    executeCommand(cmd.trim());
                }
            } else if (ACTION_ENABLE_ALWAYS_ON.equals(act)) {
                isAlwaysOn = true;
                scheduleRestartListening(200);
            }
        }
        return START_STICKY;
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "SpeechRecognizer not available on device");
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new VoiceListenerImpl(this));
    }

    public void startListening() {
        if (speechRecognizer == null) initSpeechRecognizer();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Start listening failed: " + e.getMessage());
            scheduleRestartListening(1500);
        }
    }

    public void stopListening() {
        try {
            if (speechRecognizer != null) speechRecognizer.stopListening();
        } catch (Exception ignored) {}
    }

    public void scheduleRestartListening(long delayMillis) {
        if (!isAlwaysOn) return;
        handler.removeCallbacks(restartTask);
        handler.postDelayed(restartTask, delayMillis);
    }

    public void processVoiceUtterance(String utterance) {
        if (utterance == null || utterance.trim().isEmpty()) {
            scheduleRestartListening(300);
            return;
        }

        String raw = utterance.trim();
        String lower = raw.toLowerCase();
        boolean hasWakeWord = lower.contains("skai") || lower.contains("sky") || lower.contains("jarvis") || lower.contains("friday");
        boolean inActiveConversation = (System.currentTimeMillis() - lastInteractionTime) < 35000;

        if (hasWakeWord || inActiveConversation) {
            lastInteractionTime = System.currentTimeMillis();
            String cleanCmd = raw.replaceAll("(?i)(hey\\s+skai|ok\\s+skai|skai|hey\\s+sky|sky|jarvis|hey\\s+jarvis)[,\\s]*", "").trim();
            if (cleanCmd.isEmpty()) {
                speak("Yes, sir? I am listening.");
                FloatingOverlayService.updateStatus(this, "SKAI: I am listening, sir...");
                scheduleRestartListening(1800);
            } else {
                executeCommand(cleanCmd);
            }
        } else {
            scheduleRestartListening(300);
        }
    }

    public void executeCommand(final String command) {
        lastInteractionTime = System.currentTimeMillis();
        FloatingOverlayService.updateStatus(this, "SKAI: " + command);
        MainActivity.appendLog("USER: \"" + command + "\"");

        // Try Autonomous Humanoid Playbooks first
        boolean handled = HumanoidTaskEngine.executeHumanoidTask(this, command, new HumanoidTaskCallbackImpl(this));
        if (handled) return;

        // Fallback to ReAct Orchestrator
        orchestrator.start(command);
    }

    public void speak(final String text) {
        if (ttsReady && tts != null) {
            stopListening();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "skai_tts");
            handler.postDelayed(new SpeechResumeTask(this), Math.max(1600, text.length() * 80L));
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
            tts.setPitch(1.0f);
            tts.setSpeechRate(1.05f);
            ttsReady = true;
        }
    }

    @Override
    public void onLog(String message) {
        FloatingOverlayService.updateStatus(this, message);
        MainActivity.appendLog(message);
    }

    @Override
    public void onFinished(boolean success, String summary) {
        speak(summary);
        FloatingOverlayService.updateStatus(this, (success ? "✓ " : "✗ ") + summary);
        MainActivity.appendLog("SKAI: " + summary);
        scheduleRestartListening(2500);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "SKAI Autonomous Voice Assistant",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(chan);
        }
    }

    private Notification buildNotification(String content) {
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                new Notification.Builder(this, CHANNEL_ID) :
                new Notification.Builder(this);

        return builder
                .setContentTitle("SKAI Humanoid Agent Online")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isAlwaysOn = false;
        handler.removeCallbacks(restartTask);
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) tts.shutdown();
        if (orchestrator != null) orchestrator.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
