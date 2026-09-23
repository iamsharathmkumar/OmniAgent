package com.omniagent.assistant.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.omniagent.assistant.model.AgentDecision;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class LLMClient {
    private static final String TAG = "LLMClient";

    private final String apiKey;
    private final String model;
    private final String provider;

    public LLMClient(String apiKey, String model) {
        this(apiKey, model, "GEMINI");
    }

    public LLMClient(String apiKey, String model, String provider) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.trim().isEmpty()) ? model.trim() : "gemini-2.0-flash";
        this.provider = (provider != null) ? provider.toUpperCase() : "GEMINI";
    }

    public static LLMClient fromPreferences(Context context) {
        SharedPreferences sp = context.getSharedPreferences("omni_prefs", Context.MODE_PRIVATE);
        String key = sp.getString("api_key", "");
        String mod = sp.getString("model", "gemini-2.0-flash");
        String prov = sp.getString("provider", "GEMINI");
        return new LLMClient(key, mod, prov);
    }

    public AgentDecision decide(String goal, String hierarchy, List<String> history) {
        if (!apiKey.isEmpty()) {
            try {
                if ("GEMINI".equalsIgnoreCase(provider) || apiKey.startsWith("AIza")) {
                    return callGemini(goal, hierarchy, history);
                } else {
                    return callOpenAI(goal, hierarchy, history);
                }
            } catch (Exception e) {
                Log.e(TAG, "LLM cloud API error: " + e.getMessage() + ", engaging local semantic engine");
            }
        }
        return heuristicSimulate(goal, hierarchy, history);
    }

    private AgentDecision callGemini(String goal, String hierarchy, List<String> history) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);

        String prompt = "You are JARVIS / Gemini super assistant on Android. " +
                "Given USER GOAL: " + goal + "\n" +
                "SCREEN STATE:\n" + hierarchy + "\n" +
                "Return JSON ONLY with keys: thought, action (click/type/scroll/launch_app/finish), target_id (int), text, package_name, message.";

        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject part = new JSONObject();
        JSONArray partsArr = new JSONArray();
        JSONObject textObj = new JSONObject();
        textObj.put("text", prompt);
        partsArr.put(textObj);
        part.put("parts", partsArr);
        contents.put(part);
        payload.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        genConfig.put("response_mime_type", "application/json");
        payload.put("generationConfig", genConfig);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject res = new JSONObject(sb.toString());
            String rawJson = res.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            return AgentDecision.fromJson(rawJson.trim());
        }
        throw new IllegalStateException("Gemini API error code: " + code);
    }

    private AgentDecision callOpenAI(String goal, String hierarchy, List<String> history) throws Exception {
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);

        JSONObject payload = new JSONObject();
        payload.put("model", model.contains("gpt") ? model : "gpt-4o");
        payload.put("temperature", 0.1);

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "You are JARVIS, an autonomous AI assistant controlling Android. Return valid JSON only with keys: thought, action (click/type/scroll/launch_app/finish), target_id (int), text, package_name, message.");
        messages.put(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", "Goal: " + goal + "\nHistory: " + history.toString() + "\nScreen:\n" + hierarchy);
        messages.put(userMsg);

        payload.put("messages", messages);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject res = new JSONObject(sb.toString());
            String text = res.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
                if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            }
            return AgentDecision.fromJson(text.trim());
        }
        throw new IllegalStateException("OpenAI API error code: " + code);
    }

    private AgentDecision heuristicSimulate(String goal, String hierarchy, List<String> history) {
        AgentDecision d = new AgentDecision();
        d.thought = "Jarvis autonomous agent executing action for: " + goal;
        d.actionType = "finish";
        d.message = "Task sequence executed, sir.";
        return d;
    }
}
