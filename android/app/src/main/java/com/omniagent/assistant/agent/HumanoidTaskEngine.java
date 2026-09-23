package com.omniagent.assistant.agent;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import com.omniagent.assistant.service.AgentAccessibilityService;
import java.util.List;

public class HumanoidTaskEngine {

    private static final String TAG = "HumanoidTaskEngine";

    public interface TaskCallback {
        void onProgress(String message, String speech);
        void onCompleted(boolean success, String summary, String speech);
    }

    /**
     * Executes autonomous humanoid multi-step tasks hands-free.
     */
    public static boolean executeHumanoidTask(final Context context, final String cleanCommand, final TaskCallback callback) {
        String cmd = cleanCommand.toLowerCase().trim();

        // 1. AUTONOMOUS APP INSTALLATION: "install [app name]", "download [app name]"
        if (cmd.startsWith("install ") || cmd.startsWith("download ")) {
            final String appName = cleanCommand.replaceAll("(?i)^(install|download|get)\\s+", "").trim();
            if (!appName.isEmpty()) {
                callback.onProgress("Initiating Play Store protocol for: " + appName,
                        "Right away. Accessing Google Play Store to install " + appName + " for you now.");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Step 1: Open Google Play Store search for the requested app
                            Intent playIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(appName)));
                            playIntent.setPackage("com.android.vending");
                            playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(playIntent);

                            // Step 2: Wait for Play Store UI tree to load
                            Thread.sleep(2500);

                            // Step 3: Use Accessibility Engine to locate and click "Install" button
                            AgentAccessibilityService service = AgentAccessibilityService.instance;
                            boolean clickedInstall = false;

                            if (service != null) {
                                for (int attempt = 0; attempt < 4; attempt++) {
                                    AccessibilityNodeInfo root = service.getRootInActiveWindow();
                                    if (root != null) {
                                        // First try finding direct "Install" button
                                        AccessibilityNodeInfo installBtn = findNodeWithTextOrDesc(root, "Install", "Get", "Update");
                                        if (installBtn != null) {
                                            clickedInstall = clickNode(service, installBtn);
                                            if (clickedInstall) break;
                                        }

                                        // Or click the first app search result card to open its detail page
                                        AccessibilityNodeInfo appCard = findNodeWithTextOrDesc(root, appName);
                                        if (appCard != null) {
                                            clickNode(service, appCard);
                                            Thread.sleep(1800);
                                            AccessibilityNodeInfo detailRoot = service.getRootInActiveWindow();
                                            if (detailRoot != null) {
                                                AccessibilityNodeInfo detailInstall = findNodeWithTextOrDesc(detailRoot, "Install", "Update");
                                                if (detailInstall != null) {
                                                    clickedInstall = clickNode(service, detailInstall);
                                                    if (clickedInstall) break;
                                                }
                                            }
                                        }
                                    }
                                    Thread.sleep(1200);
                                }
                            }

                            if (clickedInstall) {
                                callback.onCompleted(true, "Installation of " + appName + " engaged.",
                                        "I have located " + appName + " on the Play Store and engaged the installation process for you.");
                            } else {
                                callback.onCompleted(true, "Play Store opened for " + appName,
                                        "I have navigated to the Play Store page for " + appName + ". Please confirm installation if prompted.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Play Store install failed", e);
                            callback.onCompleted(false, "Failed to launch Play Store: " + e.getMessage(),
                                    "I encountered an issue accessing the Play Store, sir.");
                        }
                    }
                }).start();

                return true;
            }
        }

        // 2. AUTONOMOUS MEDIA PLAYBACK: "play [song/artist] on youtube"
        if (cmd.contains("play ") && cmd.contains("youtube")) {
            final String query = cleanCommand.replaceAll("(?i)(play|on youtube|on yt)", "").trim();
            callback.onProgress("Accessing YouTube for: " + query,
                    "Accessing YouTube and loading " + query + " for you.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent ytIntent = new Intent(Intent.ACTION_SEARCH);
                        ytIntent.setPackage("com.google.android.youtube");
                        ytIntent.putExtra("query", query);
                        ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(ytIntent);

                        Thread.sleep(2200);
                        AgentAccessibilityService service = AgentAccessibilityService.instance;
                        if (service != null) {
                            AccessibilityNodeInfo root = service.getRootInActiveWindow();
                            if (root != null) {
                                // Click first video card in results
                                AccessibilityNodeInfo videoCard = findFirstClickableVideoCard(root);
                                if (videoCard != null) {
                                    clickNode(service, videoCard);
                                }
                            }
                        }
                        callback.onCompleted(true, "Playing " + query + " on YouTube",
                                "Playback for " + query + " has begun.");
                    } catch (Exception e) {
                        Log.e(TAG, "YouTube playback failed", e);
                    }
                }
            }).start();
            return true;
        }

        // 3. AUTONOMOUS CAMERA SHUTTER: "take a photo", "take a picture", "take a selfie"
        if (cmd.contains("take a photo") || cmd.contains("take a picture") || cmd.contains("take picture") || cmd.contains("take photo")) {
            callback.onProgress("Engaging camera shutter...", "Engaging camera subsystem now. Smile!");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(cameraIntent);

                        Thread.sleep(1800);
                        AgentAccessibilityService service = AgentAccessibilityService.instance;
                        if (service != null) {
                            AccessibilityNodeInfo root = service.getRootInActiveWindow();
                            if (root != null) {
                                AccessibilityNodeInfo shutter = findNodeWithTextOrDesc(root, "Shutter", "Take photo", "Capture", "Camera shutter");
                                if (shutter != null) {
                                    clickNode(service, shutter);
                                } else {
                                    // Default center-bottom shutter tap coordinate
                                    int w = context.getResources().getDisplayMetrics().widthPixels;
                                    int h = context.getResources().getDisplayMetrics().heightPixels;
                                    service.clickCoordinates(w / 2f, h * 0.88f);
                                }
                            }
                        }
                        callback.onCompleted(true, "Photo captured", "Photo captured successfully.");
                    } catch (Exception e) {
                        Log.e(TAG, "Camera shutter failed", e);
                    }
                }
            }).start();
            return true;
        }

        // 4. AUTONOMOUS NAVIGATION: "navigate to [destination]", "take me to [destination]"
        if (cmd.startsWith("navigate to ") || cmd.startsWith("take me to ") || cmd.startsWith("directions to ")) {
            String dest = cleanCommand.replaceAll("(?i)^(navigate to|take me to|directions to)\\s+", "").trim();
            callback.onProgress("Calculating navigation route to: " + dest,
                    "Routing navigation to " + dest + " on Google Maps.");
            try {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(dest));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(mapIntent);
                callback.onCompleted(true, "Navigating to " + dest, "Navigation route calculated.");
                return true;
            } catch (Exception e) {
                Intent browserMap = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(dest)));
                browserMap.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserMap);
                return true;
            }
        }

        return false;
    }

    private static boolean clickNode(AgentAccessibilityService service, AccessibilityNodeInfo node) {
        if (node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }
        AccessibilityNodeInfo p = node.getParent();
        while (p != null) {
            if (p.isClickable() && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            p = p.getParent();
        }
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (r.width() > 0 && r.height() > 0) {
            return service.clickCoordinates(r.centerX(), r.centerY());
        }
        return false;
    }

    private static AccessibilityNodeInfo findNodeWithTextOrDesc(AccessibilityNodeInfo node, String... targets) {
        if (node == null) return null;
        for (String target : targets) {
            String t = target.toLowerCase();
            CharSequence text = node.getText();
            if (text != null && text.toString().toLowerCase().contains(t)) return node;

            CharSequence desc = node.getContentDescription();
            if (desc != null && desc.toString().toLowerCase().contains(t)) return node;

            String viewId = node.getViewIdResourceName();
            if (viewId != null && viewId.toLowerCase().contains(t)) return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo match = findNodeWithTextOrDesc(node.getChild(i), targets);
            if (match != null) return match;
        }
        return null;
    }

    private static AccessibilityNodeInfo findFirstClickableVideoCard(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isClickable()) {
            Rect r = new Rect();
            node.getBoundsInScreen(r);
            // Clickable card in the middle section of the screen
            if (r.top > 200 && r.height() > 100) {
                return node;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo match = findFirstClickableVideoCard(node.getChild(i));
            if (match != null) return match;
        }
        return null;
    }
}
