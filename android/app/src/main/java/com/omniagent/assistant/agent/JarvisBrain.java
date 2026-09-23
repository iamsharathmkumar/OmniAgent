package com.omniagent.assistant.agent;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import com.omniagent.assistant.model.AgentDecision;
import com.omniagent.assistant.service.AgentAccessibilityService;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarvisBrain {

    private static final String TAG = "JarvisBrain";

    public static class ExecutionResult {
        public final boolean handled;
        public final boolean success;
        public final String spokenFeedback;
        public final String detail;

        public ExecutionResult(boolean handled, boolean success, String spokenFeedback, String detail) {
            this.handled = handled;
            this.success = success;
            this.spokenFeedback = spokenFeedback;
            this.detail = detail;
        }

        public static ExecutionResult notHandled() {
            return new ExecutionResult(false, false, null, null);
        }

        public static ExecutionResult handled(boolean success, String speech, String detail) {
            return new ExecutionResult(true, success, speech, detail);
        }
    }

    /**
     * Phase 1: Direct OS Subsystem Controls (Flashlight, Volume, Calls, Alarms, Apps, Diagnostics)
     * Executes instantly like Google Assistant / Iron Man's Jarvis without needing UI tree exploration.
     */
    public static ExecutionResult tryDirectSystemControl(Context context, String rawCommand) {
        if (rawCommand == null) return ExecutionResult.notHandled();
        String cmd = rawCommand.trim().toLowerCase();

        // 1. FLASHLIGHT / TORCH
        if (cmd.contains("flashlight") || cmd.contains("torch") || cmd.contains("lumos")) {
            boolean turnOn = !cmd.contains("off") && !cmd.contains("disable") && !cmd.contains("stop");
            boolean ok = setFlashlight(context, turnOn);
            String speech = ok ? (turnOn ? "Illumination engaged, sir." : "Flashlight deactivated, sir.") :
                    "Unable to access optical subsystem, sir.";
            return ExecutionResult.handled(ok, speech, "Flashlight toggled: " + turnOn);
        }

        // 2. VOLUME & AUDIO LEVELS
        if (cmd.contains("volume") || cmd.contains("sound") || cmd.contains("mute")) {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                if (cmd.contains("up") || cmd.contains("increase") || cmd.contains("louder") || cmd.contains("raise") || cmd.contains("max")) {
                    am.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    return ExecutionResult.handled(true, "Audio levels increased, sir.", "Volume raised");
                } else if (cmd.contains("down") || cmd.contains("decrease") || cmd.contains("lower") || cmd.contains("quieter")) {
                    am.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    return ExecutionResult.handled(true, "Volume decreased, sir.", "Volume lowered");
                } else if (cmd.contains("mute") || cmd.contains("silent")) {
                    am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    return ExecutionResult.handled(true, "Audio muted, entering stealth mode, sir.", "Audio muted");
                }
            }
        }

        // 3. BATTERY & DIAGNOSTICS
        if (cmd.contains("battery") || cmd.contains("power level") || cmd.contains("status") || cmd.contains("diagnostics")) {
            int level = getBatteryPercentage(context);
            String speech = "All core systems operational, sir. Power reserves are currently at " + level + " percent.";
            return ExecutionResult.handled(true, speech, "Battery: " + level + "%");
        }

        // 4. PHONE CALLS & DIALING
        if (cmd.startsWith("call ") || cmd.startsWith("dial ") || cmd.contains("make a call to")) {
            String target = cmd.replace("make a call to", "").replace("call", "").replace("dial", "").trim();
            if (!target.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                // If contains digits, dial directly
                if (target.matches(".*\\d+.*")) {
                    callIntent.setData(Uri.parse("tel:" + target.replaceAll("[^0-9+]", "")));
                } else {
                    callIntent.setData(Uri.parse("tel:" + target));
                }
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(callIntent);
                    return ExecutionResult.handled(true, "Initiating communications protocol for " + target + ", sir.", "Dialing: " + target);
                } catch (Exception e) {
                    Log.e(TAG, "Call failed", e);
                }
            }
        }

        // 5. ALARMS & TIMERS
        if (cmd.contains("alarm") || cmd.contains("wake me up")) {
            int hour = extractHour(cmd);
            int minute = extractMinute(cmd);
            if (hour >= 0) {
                Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, hour)
                        .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "OmniAgent Alarm")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(alarmIntent);
                    String timeStr = String.format("%02d:%02d", hour, minute);
                    return ExecutionResult.handled(true, "Alarm has been scheduled for " + timeStr + ", sir.", "Alarm set: " + timeStr);
                } catch (Exception e) {
                    Log.e(TAG, "Set alarm failed", e);
                }
            }
        }

        if (cmd.contains("timer for")) {
            int seconds = extractSeconds(cmd);
            if (seconds > 0) {
                Intent timerIntent = new Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "OmniAgent Timer")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                timerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(timerIntent);
                    return ExecutionResult.handled(true, "Timer configured for " + (seconds / 60) + " minutes, sir.", "Timer set: " + seconds + "s");
                } catch (Exception e) {
                    Log.e(TAG, "Set timer failed", e);
                }
            }
        }

        // 6. SYSTEM GLOBAL NAVIGATION
        AgentAccessibilityService service = AgentAccessibilityService.instance;
        if (service != null) {
            if (cmd.equals("go home") || cmd.equals("home screen") || cmd.equals("home")) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
                return ExecutionResult.handled(true, "Returning to primary interface, sir.", "Action: Home");
            } else if (cmd.equals("go back") || cmd.equals("back")) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                return ExecutionResult.handled(true, "Navigating back, sir.", "Action: Back");
            } else if (cmd.contains("recent apps") || cmd.contains("recents") || cmd.contains("multitask")) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
                return ExecutionResult.handled(true, "Displaying active task stack, sir.", "Action: Recents");
            } else if (cmd.contains("notification") || cmd.contains("open notifications")) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                return ExecutionResult.handled(true, "Displaying notification telemetry, sir.", "Action: Notifications");
            } else if (cmd.contains("scroll down") || cmd.contains("swipe down")) {
                service.scroll(true);
                return ExecutionResult.handled(true, "Scrolling viewport down, sir.", "Action: Scroll Down");
            } else if (cmd.contains("scroll up") || cmd.contains("swipe up")) {
                service.scroll(false);
                return ExecutionResult.handled(true, "Scrolling viewport up, sir.", "Action: Scroll Up");
            }
        }

        // 7. LAUNCHING ANY INSTALLED APP BY NAME
        if (cmd.startsWith("open ") || cmd.startsWith("launch ") || cmd.startsWith("start ")) {
            String appName = cmd.replace("open", "").replace("launch", "").replace("start", "").trim();
            if (!appName.isEmpty()) {
                String pkg = findAppPackageName(context, appName);
                if (pkg != null) {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launchIntent);
                        return ExecutionResult.handled(true, "Accessing " + appName + " application now, sir.", "Launched: " + pkg);
                    }
                }
            }
        }

        // 8. GLOBAL WEB SEARCH
        if (cmd.startsWith("search for ") || cmd.startsWith("google ") || cmd.startsWith("search ")) {
            String query = cmd.replace("search for", "").replace("google", "").replace("search", "").trim();
            if (!query.isEmpty()) {
                Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
                searchIntent.putExtra("query", query);
                searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(searchIntent);
                    return ExecutionResult.handled(true, "Searching global databases for " + query + ", sir.", "Search: " + query);
                } catch (Exception e) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browserIntent);
                    return ExecutionResult.handled(true, "Accessing search results for " + query + ", sir.", "Search: " + query);
                }
            }
        }

        return ExecutionResult.notHandled();
    }

    /**
     * Phase 2: Dynamic Semantic Screen Agent (Runs inside ANY active app)
     * Reads current on-screen node hierarchy and executes clicks/types dynamically.
     */
    public static boolean executeDynamicScreenAction(AgentAccessibilityService service, String goal) {
        if (service == null) return false;
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return false;

        String lowerGoal = goal.toLowerCase();

        // 1. Text input request: "type X", "enter X", "saying X", "message X"
        if (lowerGoal.contains("type ") || lowerGoal.contains("enter ") || lowerGoal.contains("saying ") || lowerGoal.contains("message ")) {
            String textToType = extractTextToType(goal);
            if (!textToType.isEmpty()) {
                AccessibilityNodeInfo editable = findFirstEditable(root);
                if (editable != null) {
                    editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    android.os.Bundle args = new android.os.Bundle();
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType);
                    return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                }
            }
        }

        // 2. Click request: "click X", "tap X", "select X", "press X"
        String targetWord = extractTargetKeyword(goal);
        if (!targetWord.isEmpty()) {
            AccessibilityNodeInfo match = findNodeMatchingText(root, targetWord);
            if (match != null) {
                if (match.isClickable() && match.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true;
                }
                AccessibilityNodeInfo parent = match.getParent();
                while (parent != null) {
                    if (parent.isClickable() && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true;
                    }
                    parent = parent.getParent();
                }
                // Coordinate tap
                android.graphics.Rect r = new android.graphics.Rect();
                match.getBoundsInScreen(r);
                if (r.width() > 0 && r.height() > 0) {
                    return service.clickCoordinates(r.centerX(), r.centerY());
                }
            }
        }

        // 3. Default: Click Send, Done, Search or Submit button if present
        if (lowerGoal.contains("send") || lowerGoal.contains("submit") || lowerGoal.contains("done")) {
            AccessibilityNodeInfo actionBtn = findNodeMatchingKeywords(root, "send", "submit", "done", "confirm", "ok", "place order");
            if (actionBtn != null) {
                return actionBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        return false;
    }

    private static String findAppPackageName(Context context, String query) {
        String cleanQuery = query.toLowerCase().replaceAll("[^a-z0-9]", "");
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // Common popular apps dictionary for instant resolution
        if (cleanQuery.contains("youtube")) return "com.google.android.youtube";
        if (cleanQuery.contains("whatsapp")) return "com.whatsapp";
        if (cleanQuery.contains("spotify")) return "com.spotify.music";
        if (cleanQuery.contains("instagram")) return "com.instagram.android";
        if (cleanQuery.contains("uber")) return "com.ubercab";
        if (cleanQuery.contains("doordash")) return "com.doordash.android";
        if (cleanQuery.contains("netflix")) return "com.netflix.mediaclient";
        if (cleanQuery.contains("chrome")) return "com.android.chrome";
        if (cleanQuery.contains("maps")) return "com.google.android.apps.maps";
        if (cleanQuery.contains("clock")) return "com.google.android.deskclock";
        if (cleanQuery.contains("settings")) return "com.android.settings";
        if (cleanQuery.contains("camera")) return "com.google.android.GoogleCamera";
        if (cleanQuery.contains("telegram")) return "org.telegram.messenger";
        if (cleanQuery.contains("gmail")) return "com.google.android.gm";

        // Dynamic search across all installed apps
        for (ApplicationInfo info : apps) {
            String label = pm.getApplicationLabel(info).toString().toLowerCase().replaceAll("[^a-z0-9]", "");
            if (label.contains(cleanQuery) || cleanQuery.contains(label)) {
                return info.packageName;
            }
        }
        return null;
    }

    private static boolean setFlashlight(Context context, boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (cm != null) {
                    String[] ids = cm.getCameraIdList();
                    if (ids.length > 0) {
                        cm.setTorchMode(ids[0], on);
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Flashlight error", e);
            }
        }
        return false;
    }

    private static int getBatteryPercentage(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                return (int) ((level / (float) scale) * 100);
            }
        }
        return 85;
    }

    private static int extractHour(String cmd) {
        Pattern p = Pattern.compile("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?");
        Matcher m = p.matcher(cmd);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            String ampm = m.group(4);
            if ("pm".equalsIgnoreCase(ampm) && h < 12) h += 12;
            if ("am".equalsIgnoreCase(ampm) && h == 12) h = 0;
            return h;
        }
        return -1;
    }

    private static int extractMinute(String cmd) {
        Pattern p = Pattern.compile("\\d{1,2}:(\\d{2})");
        Matcher m = p.matcher(cmd);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static int extractSeconds(String cmd) {
        Pattern p = Pattern.compile("(\\d+)\\s*(minute|min|sec|second)");
        Matcher m = p.matcher(cmd);
        if (m.find()) {
            int val = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            if (unit.startsWith("min")) return val * 60;
            return val;
        }
        return 300;
    }

    private static String extractTextToType(String goal) {
        if (goal.contains("saying ")) return goal.substring(goal.indexOf("saying ") + 7).trim();
        if (goal.contains("type ")) return goal.substring(goal.indexOf("type ") + 5).trim();
        if (goal.contains("enter ")) return goal.substring(goal.indexOf("enter ") + 6).trim();
        if (goal.contains("message ")) return goal.substring(goal.indexOf("message ") + 8).trim();
        return "";
    }

    private static String extractTargetKeyword(String goal) {
        String lower = goal.toLowerCase();
        if (lower.contains("click ")) return lower.substring(lower.indexOf("click ") + 6).trim();
        if (lower.contains("tap ")) return lower.substring(lower.indexOf("tap ") + 4).trim();
        if (lower.contains("select ")) return lower.substring(lower.indexOf("select ") + 7).trim();
        if (lower.contains("press ")) return lower.substring(lower.indexOf("press ") + 6).trim();
        return "";
    }

    private static AccessibilityNodeInfo findFirstEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo res = findFirstEditable(node.getChild(i));
            if (res != null) return res;
        }
        return null;
    }

    private static AccessibilityNodeInfo findNodeMatchingText(AccessibilityNodeInfo node, String query) {
        if (node == null || query == null || query.isEmpty()) return null;
        String q = query.toLowerCase();

        CharSequence text = node.getText();
        if (text != null && text.toString().toLowerCase().contains(q)) return node;

        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.toString().toLowerCase().contains(q)) return node;

        String viewId = node.getViewIdResourceName();
        if (viewId != null && viewId.toLowerCase().contains(q)) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo res = findNodeMatchingText(node.getChild(i), query);
            if (res != null) return res;
        }
        return null;
    }

    private static AccessibilityNodeInfo findNodeMatchingKeywords(AccessibilityNodeInfo node, String... keywords) {
        if (node == null) return null;
        for (String k : keywords) {
            AccessibilityNodeInfo res = findNodeMatchingText(node, k);
            if (res != null) return res;
        }
        return null;
    }
}
