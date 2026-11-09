package com.example.studymate.ai;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class GeminiClient implements AiClient {
    private final Context context;
    private final AiModel model;

    public GeminiClient(Context context) {
        this(context, AiModel.defaultModel());
    }

    public GeminiClient(Context context, AiModel model) {
        this.context = context.getApplicationContext();
        this.model = model == null ? AiModel.defaultModel() : model;
    }

    @Override
    public void chat(String message, byte[] mediaBytes, String mimeType, String contextText, AiCallback<String> callback) {
        List<ChatMessage> history = new ArrayList<>();
        StringBuilder prompt = new StringBuilder();

        if (contextText != null && !contextText.trim().isEmpty()) {
            prompt.append("Context from study material:\n---\n").append(contextText.trim()).append("\n---\n\n");
        }
        prompt.append(message);

        history.add(new ChatMessage(ChatMessage.ROLE_USER, prompt.toString()));

        OpenRouterService.sendMessage(context, model, history, new OpenRouterService.Callback() {
            @Override
            public void onSuccess(String responseText) {
                callback.onSuccess(responseText);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    @Override
    public void explain(String topic, byte[] mediaBytes, String mimeType, String contextText, AiCallback<String> callback) {
        String msg = "Please explain the following topic clearly and simply with examples: " + topic;
        chat(msg, mediaBytes, mimeType, contextText, callback);
    }

    @Override
    public void summarize(byte[] mediaBytes, String mimeType, String contextText, AiCallback<String> callback) {
        String msg = "Please provide a concise, structured summary highlighting key concepts and bullet points from the provided material.";
        chat(msg, mediaBytes, mimeType, contextText, callback);
    }

    @Override
    public void generateQuiz(int count, String difficulty, byte[] mediaBytes, String mimeType, String contextText, AiCallback<List<QuizQuestion>> callback) {
        String prompt = "Generate a quiz with exactly " + count + " " + difficulty + " difficulty multiple-choice questions based on the material provided.\n"
                + "Return ONLY a valid JSON array of objects (no markdown, no surrounding text) with this exact schema:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"Question text\",\n"
                + "    \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n"
                + "    \"correctIndex\": 0,\n"
                + "    \"explanation\": \"Short explanation of correct answer\"\n"
                + "  }\n"
                + "]\n";

        chat(prompt, mediaBytes, mimeType, contextText, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    List<QuizQuestion> questions = QuizParser.parse(result);
                    callback.onSuccess(questions);
                } catch (Exception e) {
                    callback.onError("Failed to parse quiz response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
