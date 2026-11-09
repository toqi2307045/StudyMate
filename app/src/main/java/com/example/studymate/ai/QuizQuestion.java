package com.example.studymate.ai;

import java.util.List;

public class QuizQuestion {
    public final String question;
    public final List<String> options;
    public final int correctIndex;
    public final String explanation;

    public QuizQuestion(String question, List<String> options, int correctIndex, String explanation) {
        this.question = question;
        this.options = options;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }
}
