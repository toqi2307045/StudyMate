package com.example.studymate.ai;

public interface AiCallback<T> {
    void onSuccess(T result);
    void onError(String error);
}
