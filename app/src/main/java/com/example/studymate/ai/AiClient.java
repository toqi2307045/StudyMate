package com.example.studymate.ai;

import java.util.List;

public interface AiClient {
    void chat(String message, byte[] mediaBytes, String mimeType, String contextText, AiCallback<String> callback);
    void explain(String topic, byte[] mediaBytes, String mimeType, String contextText, AiCallback<String> callback);
    void summarize(byte[] mediaBytes, String mimeType, String contextText, AiCallback<String> callback);
    void generateQuiz(int count, String difficulty, byte[] mediaBytes, String mimeType, String contextText, AiCallback<List<QuizQuestion>> callback);
}
