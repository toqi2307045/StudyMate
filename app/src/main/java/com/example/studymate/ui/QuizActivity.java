package com.example.studymate.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import com.example.studymate.ai.QuizParser;
import com.example.studymate.ai.QuizQuestion;
import com.example.studymate.databinding.ActivityQuizBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends BaseActivity {
    public static List<QuizQuestion> currentQuiz;

    private ActivityQuizBinding b;
    private List<QuizQuestion> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "AI Practice Quiz", true);

        String jsonStr = getIntent().getStringExtra("json");
        if (jsonStr != null) {
            try {
                questions = QuizParser.parse(jsonStr);
            } catch (Exception ignored) { }
        } else if (currentQuiz != null) {
            questions = currentQuiz;
        }

        if (questions == null || questions.isEmpty()) {
            UiUtil.toast(this, "No quiz questions available");
            finish();
            return;
        }

        b.btnSubmit.setOnClickListener(v -> submitAnswer());
        b.btnNext.setOnClickListener(v -> nextQuestion());

        showQuestion(0);
    }

    private void showQuestion(int index) {
        currentIndex = index;
        QuizQuestion q = questions.get(index);

        b.tvProgress.setText("Question " + (index + 1) + " of " + questions.size());
        b.tvQuestion.setText(q.question);

        RadioButton[] rbs = {b.rbOption0, b.rbOption1, b.rbOption2, b.rbOption3};
        b.radioGroupOptions.clearCheck();

        for (int i = 0; i < 4; i++) {
            if (i < q.options.size()) {
                rbs[i].setVisibility(View.VISIBLE);
                rbs[i].setText(q.options.get(i));
                rbs[i].setTextColor(UiUtil.isNight(this) ? 0xFFFFFFFF : 0xFF000000);
                rbs[i].setEnabled(true);
            } else {
                rbs[i].setVisibility(View.GONE);
            }
        }

        b.tvExplanation.setVisibility(View.GONE);
        b.btnSubmit.setVisibility(View.VISIBLE);
        b.btnNext.setVisibility(View.GONE);
    }

    private void submitAnswer() {
        int checkedId = b.radioGroupOptions.getCheckedRadioButtonId();
        if (checkedId == -1) {
            UiUtil.toast(this, "Please select an answer");
            return;
        }

        int selectedIdx = 0;
        if (checkedId == b.rbOption1.getId()) selectedIdx = 1;
        else if (checkedId == b.rbOption2.getId()) selectedIdx = 2;
        else if (checkedId == b.rbOption3.getId()) selectedIdx = 3;

        QuizQuestion q = questions.get(currentIndex);
        RadioButton[] rbs = {b.rbOption0, b.rbOption1, b.rbOption2, b.rbOption3};

        for (RadioButton rb : rbs) rb.setEnabled(false);

        if (selectedIdx == q.correctIndex) {
            score++;
            rbs[selectedIdx].setTextColor(0xFF4CAF50);
        } else {
            rbs[selectedIdx].setTextColor(0xFFF44336);
            if (q.correctIndex >= 0 && q.correctIndex < rbs.length) {
                rbs[q.correctIndex].setTextColor(0xFF4CAF50);
            }
        }

        if (q.explanation != null && !q.explanation.trim().isEmpty()) {
            b.tvExplanation.setText("Explanation: " + q.explanation);
            b.tvExplanation.setVisibility(View.VISIBLE);
        }

        b.btnSubmit.setVisibility(View.GONE);
        b.btnNext.setVisibility(View.VISIBLE);
    }

    private void nextQuestion() {
        if (currentIndex + 1 < questions.size()) {
            showQuestion(currentIndex + 1);
        } else {
            showFinalScore();
        }
    }

    private void showFinalScore() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Quiz Completed! 🎉")
                .setMessage("Your Score: " + score + " / " + questions.size() + "\n\nGreat job practicing your course material!")
                .setPositiveButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}
