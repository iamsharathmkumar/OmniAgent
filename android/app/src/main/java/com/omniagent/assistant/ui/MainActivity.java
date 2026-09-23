package com.omniagent.assistant.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.omniagent.assistant.R;
import com.omniagent.assistant.service.AgentAccessibilityService;
import com.omniagent.assistant.service.FloatingOverlayService;
import com.omniagent.assistant.service.VoiceAssistantService;
import com.omniagent.assistant.util.PermissionHelper;

public class MainActivity extends Activity implements View.OnClickListener {

    private static MainActivity activeInstance = null;

    private TextView tvServiceStatus;
    private TextView tvCoreStatus;
    private TextView tvLogs;
    private ScrollView scrollLogs;
    private EditText etCommand;
    private EditText etApiKey;

    private static class AppendLogRunner implements Runnable {
        private final String text;
        AppendLogRunner(String text) {
            this.text = text;
        }
        @Override
        public void run() {
            if (activeInstance != null && activeInstance.tvLogs != null) {
                activeInstance.tvLogs.append("\n" + text);
                if (activeInstance.scrollLogs != null) {
                    activeInstance.scrollLogs.fullScroll(View.FOCUS_DOWN);
                }
            }
        }
    }

    public static void appendLog(String text) {
        if (activeInstance != null) {
            activeInstance.runOnUiThread(new AppendLogRunner(text));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = this;
        setContentView(R.layout.activity_main);

        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvCoreStatus = findViewById(R.id.tv_core_status);
        tvLogs = findViewById(R.id.tv_logs);
        scrollLogs = findViewById(R.id.scroll_logs);
        etCommand = findViewById(R.id.et_command);
        etApiKey = findViewById(R.id.et_api_key);

        SharedPreferences sp = getSharedPreferences("omni_prefs", Context.MODE_PRIVATE);
        String savedKey = sp.getString("api_key", "");
        if (!savedKey.isEmpty()) {
            etApiKey.setText(savedKey);
        }

        // Start hands-free background service
        Intent startVoice = new Intent(this, VoiceAssistantService.class);
        startVoice.setAction(VoiceAssistantService.ACTION_ENABLE_ALWAYS_ON);
        startService(startVoice);

        findViewById(R.id.btn_toggle_overlay).setOnClickListener(this);
        findViewById(R.id.btn_enable_accessibility).setOnClickListener(this);
        findViewById(R.id.btn_grant_overlay).setOnClickListener(this);
        findViewById(R.id.btn_grant_audio).setOnClickListener(this);
        findViewById(R.id.btn_mic).setOnClickListener(this);
        findViewById(R.id.btn_execute).setOnClickListener(this);
        findViewById(R.id.btn_save_key).setOnClickListener(this);

        findViewById(R.id.btn_chip_install).setOnClickListener(this);
        findViewById(R.id.btn_chip_youtube).setOnClickListener(this);
        findViewById(R.id.btn_chip_photo).setOnClickListener(this);
        findViewById(R.id.btn_chip_torch).setOnClickListener(this);
        findViewById(R.id.btn_chip_volume).setOnClickListener(this);
        findViewById(R.id.btn_chip_battery).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_toggle_overlay) {
            if (PermissionHelper.canDrawOverlays(this)) {
                Intent intent = new Intent(this, FloatingOverlayService.class);
                startService(intent);
                Toast.makeText(this, "SKAI Arc HUD Engaged", Toast.LENGTH_SHORT).show();
                appendLog("Holographic Arc HUD projected on display.");
            } else {
                Toast.makeText(this, "Please grant Overlay permission first", Toast.LENGTH_LONG).show();
                PermissionHelper.openOverlaySettings(this);
            }
        } else if (id == R.id.btn_enable_accessibility) {
            PermissionHelper.openAccessibilitySettings(this);
        } else if (id == R.id.btn_grant_overlay) {
            PermissionHelper.openOverlaySettings(this);
        } else if (id == R.id.btn_grant_audio) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.CALL_PHONE
                }, 101);
            }
        } else if (id == R.id.btn_mic) {
            if (!PermissionHelper.hasAudioPermission(this)) {
                Toast.makeText(this, "Microphone permission required for hands-free voice", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
                }
                return;
            }
            Intent intent = new Intent(this, VoiceAssistantService.class);
            intent.setAction(VoiceAssistantService.ACTION_START_LISTEN);
            startService(intent);
            appendLog("Listening for voice instruction, sir...");
        } else if (id == R.id.btn_execute) {
            String cmd = etCommand.getText().toString().trim();
            if (!cmd.isEmpty()) {
                triggerCommand(cmd);
                etCommand.setText("");
            }
        } else if (id == R.id.btn_save_key) {
            String key = etApiKey.getText().toString().trim();
            SharedPreferences sp = getSharedPreferences("omni_prefs", Context.MODE_PRIVATE);
            sp.edit().putString("api_key", key).apply();
            Toast.makeText(this, "Gemini AI Brain Key Saved!", Toast.LENGTH_SHORT).show();
            appendLog("Gemini Brain Key stored.");
        } else if (id == R.id.btn_chip_install) {
            triggerCommand("SKAI, install Subway Surfers");
        } else if (id == R.id.btn_chip_youtube) {
            triggerCommand("SKAI, play Interstellar soundtrack on YouTube");
        } else if (id == R.id.btn_chip_photo) {
            triggerCommand("SKAI, take a photo");
        } else if (id == R.id.btn_chip_torch) {
            triggerCommand("SKAI, turn on flashlight");
        } else if (id == R.id.btn_chip_volume) {
            triggerCommand("SKAI, volume up");
        } else if (id == R.id.btn_chip_battery) {
            triggerCommand("SKAI, battery diagnostics");
        }
    }

    private void triggerCommand(String command) {
        Intent intent = new Intent(this, VoiceAssistantService.class);
        intent.setAction(VoiceAssistantService.ACTION_RUN_TEXT);
        intent.putExtra(VoiceAssistantService.EXTRA_COMMAND, command);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceState();
    }

    private void updateServiceState() {
        boolean running = AgentAccessibilityService.instance != null;
        if (running) {
            tvServiceStatus.setText("Accessibility Subsystem: ACTIVE (Can control any screen)");
            tvServiceStatus.setTextColor(Color.parseColor("#34D399"));
            tvCoreStatus.setText("HANDS-FREE WAKE: ACTIVE (Say 'SKAI')");
            tvCoreStatus.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvServiceStatus.setText("Accessibility Subsystem: OFFLINE (Tap button 1 above)");
            tvServiceStatus.setTextColor(Color.parseColor("#F87171"));
            tvCoreStatus.setText("HANDS-FREE WAKE: LIMITED (Enable accessibility)");
            tvCoreStatus.setTextColor(Color.parseColor("#FBBF24"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeInstance == this) {
            activeInstance = null;
        }
    }
}
