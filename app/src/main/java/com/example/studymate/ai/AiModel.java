package com.example.studymate.ai;

import java.util.Arrays;
import java.util.List;

public class AiModel {
    public final String id;
    public final String name;
    public final String provider;
    public final String modelId;
    public final boolean isFree;
    public final String description;

    public AiModel(String id, String name, String provider, String modelId, boolean isFree, String description) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.modelId = modelId;
        this.isFree = isFree;
        this.description = description;
    }

    public static final List<AiModel> DEFAULT_MODELS = Arrays.asList(
            new AiModel("gemini_flash", "Gemini 2.0 Flash", "Google", "google/gemini-2.0-flash-001", true, "Fast & intelligent model by Google"),
            new AiModel("gemini_1_5", "Gemini 1.5 Flash", "Google", "google/gemini-flash-1.5", true, "High-speed multimodal model"),
            new AiModel("deepseek_chat", "DeepSeek V3", "DeepSeek", "deepseek/deepseek-chat", true, "Advanced reasoning & problem-solving model"),
            new AiModel("deepseek_r1", "DeepSeek R1", "DeepSeek", "deepseek/deepseek-r1", true, "DeepSeek R1 reasoning model"),
            new AiModel("llama_3_3", "Llama 3.3 70B", "Meta", "meta-llama/llama-3.3-70b-instruct", true, "Open-source 70B parameter general model"),
            new AiModel("gpt_4o_mini", "GPT-4o Mini", "OpenAI", "openai/gpt-4o-mini", false, "Fast & smart compact model by OpenAI"),
            new AiModel("claude_sonnet", "Claude 3.5 Sonnet", "Anthropic", "anthropic/claude-3.5-sonnet", false, "Top-tier coding & reasoning by Anthropic")
    );

    public static AiModel defaultModel() {
        return DEFAULT_MODELS.get(0);
    }

    public static AiModel findById(String id) {
        if (id == null) return defaultModel();
        for (AiModel m : DEFAULT_MODELS) {
            if (m.id.equals(id) || m.modelId.equals(id)) return m;
        }
        return defaultModel();
    }

    @Override
    public String toString() {
        return name + (isFree ? " (Free)" : "");
    }
}
