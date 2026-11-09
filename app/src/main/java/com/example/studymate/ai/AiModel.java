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
            new AiModel("gemini_flash", "Gemini 2.0 Flash", "Google", "google/gemini-2.0-flash-exp:free", true, "Fast & intelligent free model by Google"),
            new AiModel("deepseek_r1", "DeepSeek R1", "DeepSeek", "deepseek/deepseek-r1:free", true, "Advanced reasoning & problem-solving model"),
            new AiModel("llama_3_3", "Llama 3.3 70B", "Meta", "meta-llama/llama-3.3-70b-instruct:free", true, "Open-source 70B parameter general model"),
            new AiModel("mistral_7b", "Mistral 7B", "Mistral", "mistralai/mistral-7b-instruct:free", true, "Compact & fast open model"),
            new AiModel("gpt_4o_mini", "GPT-4o Mini", "OpenAI", "openai/gpt-4o-mini", false, "Fast & smart compact model by OpenAI"),
            new AiModel("claude_sonnet", "Claude 3.5 Sonnet", "Anthropic", "anthropic/claude-3.5-sonnet", false, "Top-tier coding & reasoning by Anthropic"),
            new AiModel("grok_2", "Grok 2", "xAI", "xai/grok-2", false, "Grok 2 model by xAI")
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
