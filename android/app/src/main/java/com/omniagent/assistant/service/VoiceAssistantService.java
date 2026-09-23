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
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.omniagent.assistant.agent.AgentOrchestrator;
import com.omniagent.assistant.agent.LLMClient;
import com.omniagent.assistant.ui.MainActivity;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceAssistantService extends Service implements TextToSpeech.OnInitListener, AgentOrchestrator.OrchestratorListener {

    private static final String TAG = "VoiceAssistant";
    public static final String ACTION_START_LISTEN = "com.omniagent.ACTION_START_LISTEN";
    public static final String ACTION_STOP = "com.omniagent.ACTION_STOP";
    public static final String ACTION_RUN_TEXT = "com.omniagent.ACTION_RUN_TEXT";
    public static final String EXTRA_COMMAND = "extra_command";

    private static final String CHANNEL_ID = "omni_voice_channel";
    private static final int NOTIF_ID = 2001;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private AgentOrchestrator orchestrator;

    private static class VoiceListenerImpl implements RecognitionListener {
        private final VoiceAssistantService service;

        VoiceListenerImpl(VoiceAssistantService service) {
            this.service = service;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            FloatingOverlayService.updateStatus(service, "Listening...");
        }
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {
            FloatingOverlayService.updateStatus(service, "Processing audio...");
        }
        @Override public void onError(int error) {
            FloatingOverlayService.updateStatus(service, "Ready (Tap to speak)");
        }
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String cmd = matches.get(0);
                service.executeCommand(cmd);
            }
        }
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("OmniAgent is standing by"));

        tts = new TextToSpeech(this, this);

        LLMClient llmClient = LLMClient.fromPreferences(this);
        orchestrator = new AgentOrchestrator(this, llmClient, this);

        initSpeechRecognizer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String act = intent.getAction();
            if (ACTION_START_LISTEN.equals(act)) {
                startListening();
            } else if (ACTION_STOP.equals(act)) {
                if (orchestrator != null) orchestrator.stop();
            } else if (ACTION_RUN_TEXT.equals(act)) {
                String cmd = intent.getStringExtra(EXTRA_COMMAND);
                if (cmd != null && !cmd.trim().isEmpty()) {
                    executeCommand(cmd.trim());
                }
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
        }
    }

    public void executeCommand(String command) {
        FloatingOverlayService.updateStatus(this, "JARVIS: " + command);
        speak("Right away, sir.");
        orchestrator.start(command);
    }

    public void speak(String text) {
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "omni_tts");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
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
        MainActivity.appendLog("FINISHED: " + summary);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "OmniAgent Voice Listener",
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
                .setContentTitle("OmniAgent AI Active")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) tts.shutdown();
        if (orchestrator != null) orchestrator.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
