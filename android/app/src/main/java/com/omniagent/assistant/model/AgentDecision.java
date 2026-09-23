package com.omniagent.assistant.model;

import org.json.JSONObject;

public class AgentDecision {
    public String thought = "";
    public String actionType = "finish";
    public int targetId = -1;
    public float coordX = -1;
    public float coordY = -1;
    public String text = null;
    public String direction = "down";
    public String packageName = null;
    public String message = null;
    public boolean success = true;

    public static AgentDecision fromJson(String jsonStr) {
        AgentDecision d = new AgentDecision();
        try {
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has("thought")) d.thought = obj.getString("thought");
            if (obj.has("action")) d.actionType = obj.getString("action");
            if (obj.has("target_id")) d.targetId = obj.getInt("target_id");
            if (obj.has("text")) d.text = obj.getString("text");
            if (obj.has("direction")) d.direction = obj.getString("direction");
            if (obj.has("package_name")) d.packageName = obj.getString("package_name");
            if (obj.has("message")) d.message = obj.getString("message");
            if (obj.has("success")) d.success = obj.getBoolean("success");
            if (obj.has("coordinates")) {
                org.json.JSONArray arr = obj.getJSONArray("coordinates");
                if (arr.length() >= 2) {
                    d.coordX = (float) arr.getDouble(0);
                    d.coordY = (float) arr.getDouble(1);
                }
            }
        } catch (Exception e) {
            d.thought = "Heuristic decision parsed";
            d.actionType = "finish";
            d.message = "Finished processing.";
        }
        return d;
    }
}
