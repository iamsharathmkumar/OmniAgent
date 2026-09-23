package com.omniagent.assistant.agent;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.omniagent.assistant.model.AgentDecision;
import com.omniagent.assistant.service.AgentAccessibilityService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentOrchestrator {

    public interface OrchestratorListener {
        void onLog(String message);
        void onFinished(boolean success, String summary);
    }

    private final Context context;
    private final LLMClient llmClient;
    private final OrchestratorListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public AgentOrchestrator(Context context, LLMClient llmClient, OrchestratorListener listener) {
        this.context = context;
        this.llmClient = llmClient;
        this.listener = listener;
    }

    private static class TaskRunner implements Runnable {
        private final AgentOrchestrator orchestrator;
        private final String goal;

        TaskRunner(AgentOrchestrator orchestrator, String goal) {
            this.orchestrator = orchestrator;
            this.goal = goal;
        }

        @Override
        public void run() {
            try {
                orchestrator.runLoop(goal);
            } catch (Exception e) {
                orchestrator.postLog("Subsystem anomaly: " + e.getMessage());
                orchestrator.postFinish(false, "Error: " + e.getMessage());
            } finally {
                orchestrator.isRunning.set(false);
            }
        }
    }

    public void start(String goal) {
        if (isRunning.getAndSet(true)) {
            stop();
            isRunning.set(true);
        }
        executor.execute(new TaskRunner(this, goal));
    }

    public void stop() {
        isRunning.set(false);
    }

    private void runLoop(String goal) {
        postLog("Jarvis Command Received: \"" + goal + "\"");

        // Step 1: Check for Direct Jarvis Subsystem Control (Flashlight, Volume, Apps, Alarms, Calls)
        JarvisBrain.ExecutionResult direct = JarvisBrain.tryDirectSystemControl(context, goal);
        if (direct.handled) {
            postLog("Status: " + direct.detail);
            postFinish(direct.success, direct.spokenFeedback);
            return;
        }

        // Step 2: Check Accessibility Engine for Universal App Interaction
        AgentAccessibilityService service = AgentAccessibilityService.instance;
        if (service == null) {
            postLog("Jarvis Accessibility Subsystem is not engaged. Please enable in Settings.");
            postFinish(false, "Accessibility service is required to control on-screen applications, sir.");
            return;
        }

        // Step 3: Dynamic Screen Execution
        postLog("Analyzing visual telemetry on active screen...");
        boolean dynamicHandled = JarvisBrain.executeDynamicScreenAction(service, goal);
        if (dynamicHandled) {
            postLog("Successfully executed action on active application.");
            postFinish(true, "Action executed successfully on screen, sir.");
            return;
        }

        // Step 4: Full Multi-Hop ReAct Loop (via Gemini / OpenAI or Semantic Planner)
        List<String> history = new ArrayList<String>();
        int step = 0;
        int maxSteps = 10;

        while (isRunning.get() && step < maxSteps) {
            step++;
            postLog("Step " + step + ": Inspecting UI hierarchy...");

            UIHierarchyParser.ParseResult screen = service.inspectScreen();
            AgentDecision decision = llmClient.decide(goal, screen.promptText, history);

            postLog("Jarvis Thought: " + decision.thought);
            postLog("Executing: " + decision.actionType + (decision.targetId > 0 ? " [Target: " + decision.targetId + "]" : ""));

            if ("finish".equalsIgnoreCase(decision.actionType)) {
                postFinish(decision.success, decision.message != null ? decision.message : "Task completed, sir.");
                return;
            }

            boolean ok = false;
            String act = decision.actionType.toLowerCase();
            if ("click".equals(act)) {
                if (decision.targetId > 0) {
                    ok = service.clickElement(decision.targetId);
                } else if (decision.coordX >= 0 && decision.coordY >= 0) {
                    ok = service.clickCoordinates(decision.coordX, decision.coordY);
                }
            } else if ("type".equals(act)) {
                ok = service.inputText(decision.targetId, decision.text != null ? decision.text : "");
            } else if ("scroll".equals(act)) {
                ok = service.scroll("down".equalsIgnoreCase(decision.direction));
            } else if ("launch_app".equals(act)) {
                ok = decision.packageName != null && service.launchApp(decision.packageName);
            }

            history.add("Step " + step + ": " + decision.actionType + " -> " + (ok ? "ok" : "failed"));

            try {
                Thread.sleep(1100);
            } catch (InterruptedException ignored) {}
        }

        if (step >= maxSteps) {
            postFinish(false, "Task sequence concluded, sir.");
        }
    }

    private static class LogPoster implements Runnable {
        private final OrchestratorListener listener;
        private final String message;
        LogPoster(OrchestratorListener listener, String message) {
            this.listener = listener;
            this.message = message;
        }
        @Override
        public void run() {
            if (listener != null) listener.onLog(message);
        }
    }

    private static class FinishPoster implements Runnable {
        private final OrchestratorListener listener;
        private final boolean success;
        private final String summary;
        FinishPoster(OrchestratorListener listener, boolean success, String summary) {
            this.listener = listener;
            this.success = success;
            this.summary = summary;
        }
        @Override
        public void run() {
            if (listener != null) listener.onFinished(success, summary);
        }
    }

    private void postLog(String msg) {
        mainHandler.post(new LogPoster(listener, msg));
    }

    private void postFinish(boolean success, String summary) {
        mainHandler.post(new FinishPoster(listener, success, summary));
    }
}
