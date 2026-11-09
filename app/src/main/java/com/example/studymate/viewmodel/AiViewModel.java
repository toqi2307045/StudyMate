package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studymate.ai.AiCallback;
import com.example.studymate.ai.AiModel;
import com.example.studymate.ai.ChatMessage;
import com.example.studymate.ai.ContentExtractor;
import com.example.studymate.ai.GeminiClient;
import com.example.studymate.ai.QuizQuestion;
import com.example.studymate.util.Prefs;

import java.util.ArrayList;
import java.util.List;

public class AiViewModel extends AndroidViewModel {
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<AiModel> selectedModel = new MutableLiveData<>();
    private final MutableLiveData<ContentExtractor.ExtractedContent> attachedContent = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<List<QuizQuestion>> generatedQuiz = new MutableLiveData<>(null);

    public AiViewModel(@NonNull Application application) {
        super(application);
        String savedModelId = Prefs.selectedAiModelId(application);
        selectedModel.setValue(AiModel.findById(savedModelId));

        List<ChatMessage> list = new ArrayList<>();
        list.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                "Hello! I am your AI Study Tutor. Ask me any study question, attach a course document, or generate a practice quiz!"));
        messages.setValue(list);
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<AiModel> getSelectedModel() { return selectedModel; }
    public LiveData<ContentExtractor.ExtractedContent> getAttachedContent() { return attachedContent; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<QuizQuestion>> getGeneratedQuiz() { return generatedQuiz; }

    public void selectModel(AiModel model) {
        if (model == null) return;
        selectedModel.setValue(model);
        Prefs.setSelectedAiModelId(getApplication(), model.id);
    }

    public void setAttachedContent(ContentExtractor.ExtractedContent content) {
        attachedContent.setValue(content);
        if (content != null) {
            List<ChatMessage> list = messages.getValue() == null ? new ArrayList<>() : new ArrayList<>(messages.getValue());
            list.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                    "Attached study document: " + content.fileName + ". You can now ask questions, summarize, or generate a quiz on this file!"));
            messages.setValue(list);
        }
    }

    public void sendMessage(String userText) {
        if (userText == null || userText.trim().isEmpty()) return;
        if (Boolean.TRUE.equals(isLoading.getValue())) return;

        String query = userText.trim();
        List<ChatMessage> list = messages.getValue() == null ? new ArrayList<>() : new ArrayList<>(messages.getValue());
        list.add(new ChatMessage(ChatMessage.ROLE_USER, query));
        messages.setValue(list);

        isLoading.setValue(true);
        errorMessage.setValue(null);

        ContentExtractor.ExtractedContent ext = attachedContent.getValue();
        byte[] bytes = ext != null ? ext.bytes : null;
        String mime = ext != null ? ext.mimeType : null;
        String textCtx = ext != null ? ext.text : null;

        GeminiClient client = new GeminiClient(getApplication(), selectedModel.getValue());
        client.chat(query, bytes, mime, textCtx, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                isLoading.setValue(false);
                List<ChatMessage> updated = messages.getValue() == null ? new ArrayList<>() : new ArrayList<>(messages.getValue());
                updated.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT, result, System.currentTimeMillis(), selectedModel.getValue().name));
                messages.setValue(updated);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
                List<ChatMessage> updated = messages.getValue() == null ? new ArrayList<>() : new ArrayList<>(messages.getValue());
                updated.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                        "⚠️ Unable to get AI response: " + error + "\n\nTip: Tap 'API Key Settings' in the top right menu to set an OpenRouter API key.",
                        System.currentTimeMillis(), "System"));
                messages.setValue(updated);
            }
        });
    }

    public void summarize() {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        ContentExtractor.ExtractedContent ext = attachedContent.getValue();
        byte[] bytes = ext != null ? ext.bytes : null;
        String mime = ext != null ? ext.mimeType : null;
        String textCtx = ext != null ? ext.text : null;

        GeminiClient client = new GeminiClient(getApplication(), selectedModel.getValue());
        client.summarize(bytes, mime, textCtx, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                isLoading.setValue(false);
                List<ChatMessage> updated = messages.getValue() == null ? new ArrayList<>() : new ArrayList<>(messages.getValue());
                updated.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT, "📝 **Summary:**\n\n" + result, System.currentTimeMillis(), selectedModel.getValue().name));
                messages.setValue(updated);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
                List<ChatMessage> updated = messages.getValue() == null ? new ArrayList<>() : new ArrayList<>(messages.getValue());
                updated.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                        "⚠️ Unable to generate summary: " + error + "\n\nTip: Tap 'API Key Settings' in the top right menu to set an OpenRouter API key.",
                        System.currentTimeMillis(), "System"));
                messages.setValue(updated);
            }
        });
    }

    public void generateQuiz(int count, String difficulty) {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        ContentExtractor.ExtractedContent ext = attachedContent.getValue();
        byte[] bytes = ext != null ? ext.bytes : null;
        String mime = ext != null ? ext.mimeType : null;
        String textCtx = ext != null ? ext.text : null;

        GeminiClient client = new GeminiClient(getApplication(), selectedModel.getValue());
        client.generateQuiz(count, difficulty, bytes, mime, textCtx, new AiCallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> questions) {
                isLoading.setValue(false);
                generatedQuiz.setValue(questions);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
                List<ChatMessage> updated = messages.getValue() == null ? new ArrayList<>() : new ArrayList<>(messages.getValue());
                updated.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                        "⚠️ Unable to generate quiz: " + error + "\n\nTip: Tap 'API Key Settings' in the top right menu to set an OpenRouter API key.",
                        System.currentTimeMillis(), "System"));
                messages.setValue(updated);
            }
        });
    }

    public void clearQuiz() {
        generatedQuiz.setValue(null);
    }

    public void clearChat() {
        List<ChatMessage> list = new ArrayList<>();
        list.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT,
                "Chat cleared. Ask me any study question or attach a document to get started!"));
        messages.setValue(list);
        errorMessage.setValue(null);
    }
}
