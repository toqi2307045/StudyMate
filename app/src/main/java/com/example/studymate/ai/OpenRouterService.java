package com.example.studymate.ai;

import android.content.Context;

import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OpenRouterService {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    public interface Callback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public static void sendMessage(Context context, AiModel model, List<ChatMessage> history, Callback callback) {
        String apiKey = Prefs.openRouterKey(context);
        AppExecutors.io(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("HTTP-Referer", "https://github.com/studymate");
                conn.setRequestProperty("X-Title", "StudyMate Android");

                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                }

                conn.setConnectTimeout(30000);
                conn.setReadTimeout(45000);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("model", model.modelId);

                JSONArray messagesArray = new JSONArray();

                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", ChatMessage.ROLE_SYSTEM);
                systemMsg.put("content", "You are StudyMate AI, an intelligent, helpful, concise study assistant for students. Answer only from the provided study material when given. Cite slides, pages, or sections where applicable. Explain concepts, answer questions, summarize material, and generate quizzes accurately.");
                messagesArray.put(systemMsg);

                for (ChatMessage msg : history) {
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("role", msg.role);
                    msgObj.put("content", msg.content);
                    messagesArray.put(msgObj);
                }

                payload.put("messages", messagesArray);

                byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                }

                int statusCode = conn.getResponseCode();
                InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();

                StringBuilder sb = new StringBuilder();
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                }

                String responseStr = sb.toString();

                if (statusCode >= 200 && statusCode < 300) {
                    JSONObject jsonResp = new JSONObject(responseStr);
                    JSONArray choices = jsonResp.optJSONArray("choices");
                    if (choices != null && choices.length() > 0) {
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject message = choice.optJSONObject("message");
                        if (message != null) {
                            String reply = message.optString("content", "");
                            AppExecutors.main(() -> callback.onSuccess(reply));
                            return;
                        }
                    }
                    AppExecutors.main(() -> callback.onError("No response text returned from model."));
                } else {
                    String errorMsg = "API Error (" + statusCode + ")";
                    try {
                        JSONObject errJson = new JSONObject(responseStr);
                        if (errJson.has("error")) {
                            JSONObject errObj = errJson.getJSONObject("error");
                            if (errObj.has("message")) {
                                errorMsg = errObj.getString("message");
                            }
                        }
                    } catch (Exception ignored) { }
                    if (statusCode == 401 || errorMsg.toLowerCase().contains("key")) {
                        errorMsg = "OpenRouter API Key required. Tap 'API Key Settings' in the top right menu to enter your API key from openrouter.ai/keys.";
                    }
                    final String finalErr = errorMsg;
                    AppExecutors.main(() -> callback.onError(finalErr));
                }
            } catch (Exception e) {
                final String errMsg = "Network request failed: " + (e.getMessage() != null ? e.getMessage() : e.toString());
                AppExecutors.main(() -> callback.onError(errMsg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}
