package com.omniagent.assistant.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingOverlayService extends Service implements View.OnClickListener, View.OnTouchListener {

    public static final String ACTION_UPDATE_TEXT = "com.omniagent.UPDATE_OVERLAY";
    public static final String EXTRA_TEXT = "extra_text";

    private WindowManager windowManager;
    private View overlayView;
    private TextView statusTextView;
    private TextView arcReactorIcon;
    private WindowManager.LayoutParams windowParams;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    public static void updateStatus(Context context, String text) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        intent.setAction(ACTION_UPDATE_TEXT);
        intent.putExtra(EXTRA_TEXT, text);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE_TEXT.equals(intent.getAction())) {
            String text = intent.getStringExtra(EXTRA_TEXT);
            if (statusTextView != null && text != null) {
                statusTextView.setText(text);
            }
        }
        return START_STICKY;
    }

    private void buildOverlay() {
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.TOP | Gravity.START;
        windowParams.x = 40;
        windowParams.y = 140;

        // Jarvis Arc Reactor Glowing Pill Container
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(28, 16, 28, 16);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E6090D16")); // High-tech deep obsidian
        bg.setCornerRadius(45f);
        bg.setStroke(3, Color.parseColor("#00E5FF")); // Jarvis Cyan glow
        layout.setBackground(bg);

        // Arc Reactor Glowing Core Icon
        arcReactorIcon = new TextView(this);
        arcReactorIcon.setId(1001);
        arcReactorIcon.setText("◉ ");
        arcReactorIcon.setTextColor(Color.parseColor("#00E5FF"));
        arcReactorIcon.setTextSize(18f);
        arcReactorIcon.setOnClickListener(this);
        layout.addView(arcReactorIcon);

        statusTextView = new TextView(this);
        statusTextView.setText("JARVIS Online");
        statusTextView.setTextColor(Color.parseColor("#E0F7FA"));
        statusTextView.setTextSize(12f);
        statusTextView.setMaxLines(2);
        layout.addView(statusTextView);

        TextView stopIcon = new TextView(this);
        stopIcon.setId(1002);
        stopIcon.setText("  ✕");
        stopIcon.setTextColor(Color.parseColor("#FF5252"));
        stopIcon.setTextSize(14f);
        stopIcon.setOnClickListener(this);
        layout.addView(stopIcon);

        layout.setOnTouchListener(this);

        overlayView = layout;
        windowManager.addView(overlayView, windowParams);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == 1001) {
            if (statusTextView != null) statusTextView.setText("Listening, sir...");
            Intent voiceIntent = new Intent(this, VoiceAssistantService.class);
            voiceIntent.setAction(VoiceAssistantService.ACTION_START_LISTEN);
            startService(voiceIntent);
        } else if (id == 1002) {
            if (statusTextView != null) statusTextView.setText("Standby, sir.");
            Intent stopIntent = new Intent(this, VoiceAssistantService.class);
            stopIntent.setAction(VoiceAssistantService.ACTION_STOP);
            startService(stopIntent);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = windowParams.x;
                initialY = windowParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                windowParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                windowParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                windowManager.updateViewLayout(overlayView, windowParams);
                return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
