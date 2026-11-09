package com.example.studymate.ai;

public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    public final String role;
    public final String content;
    public final long timestamp;
    public final String modelName;

    public ChatMessage(String role, String content, long timestamp, String modelName) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.modelName = modelName;
    }

    public ChatMessage(String role, String content) {
        this(role, content, System.currentTimeMillis(), null);
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
}
