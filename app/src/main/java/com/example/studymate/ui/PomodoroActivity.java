package com.example.studymate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.lifecycle.ViewModelProvider;

import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.StudySession;
import com.example.studymate.databinding.ActivityPomodoroBinding;
import com.example.studymate.util.Prefs;
import com.example.studymate.viewmodel.PomodoroViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PomodoroActivity extends BaseActivity {
    private static final int M_SETTINGS = 1;

    /** {label, focus, short break, long break, sessions before long break} */
    private static final String[] PRESET_NAMES = {
            "Custom", "Classic  (25 / 5 / 15)", "Long focus  (50 / 10 / 20)", "Deep work  (90 / 15 / 30)"};
    private static final int[][] PRESETS = {
            null, {25, 5, 15, 4}, {50, 10, 20, 3}, {90, 15, 30, 2}};

    private ActivityPomodoroBinding b;
    private PomodoroViewModel vm;
    private List<Course> courses = new ArrayList<>();
    private List<StudySession> sessions = new ArrayList<>();
    private int rangeIndex = 0; // 0 = last 7 days, 1 = all time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPomodoroBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Pomodoro Study Timer", true);
        vm = new ViewModelProvider(this).get(PomodoroViewModel.class);

        com.google.android.material.button.MaterialButtonToggleGroup modeToggle = UiUtil.segmented(this,
                new String[]{"Focus", "Short break", "Long break"},
                vm.phase.getValue() == null ? PomodoroViewModel.FOCUS : vm.phase.getValue(),
                i -> vm.setPhase(i));

        android.widget.FrameLayout.LayoutParams modeLp = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        modeLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        b.modeHolder.addView(modeToggle, modeLp);

        vm.remainingMs.observe(this, ms -> {
            long s = (ms + 999) / 1000;
            b.timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60));
        });
        vm.phase.observe(this, p -> {
            b.phaseLabel.setText(p == PomodoroViewModel.FOCUS ? "🎯 Focus" : p == PomodoroViewModel.SHORT_BREAK ? "☕ Short break" : "🌿 Long break");
            b.courseSpinner.setEnabled(p == PomodoroViewModel.FOCUS);
            if (p >= 0 && p < modeToggle.getChildCount()) {
                int targetId = modeToggle.getChildAt(p).getId();
                if (modeToggle.getCheckedButtonId() != targetId) {
                    modeToggle.check(targetId);
                }
            }
        });
        vm.completedFocus.observe(this, n -> updateCycleText());
        vm.running.observe(this, run -> {
            b.btnStart.setText(run ? "Pause" : "Start");
            if (run) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });

        b.btnStart.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(vm.running.getValue())) vm.pause(); else vm.start();
        });
        b.btnReset.setOnClickListener(v -> vm.reset());
        b.btnSkip.setOnClickListener(v -> vm.skip());
        b.btnSettings.setOnClickListener(v -> showSettings());
        b.timerText.setOnClickListener(v -> showSettings());
        updateSummary();

        b.rangeHolder.addView(UiUtil.segmented(this, new String[]{"Last 7 days", "All time"}, rangeIndex, i -> {
            rangeIndex = i;
            drawStats();
        }));

        b.courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                vm.courseId = pos <= 0 || pos > courses.size() ? 0 : courses.get(pos - 1).id;
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        vm.courses.observe(this, list -> {
            courses = list;
            List<String> names = new ArrayList<>();
            names.add("(No course)");
            int sel = 0;
            for (int i = 0; i < list.size(); i++) {
                names.add(list.get(i).label());
                if (list.get(i).id == vm.courseId) sel = i + 1;
            }
            ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            b.courseSpinner.setAdapter(ad);
            b.courseSpinner.setSelection(sel);
            drawStats();
        });
        vm.sessions.observe(this, list -> { sessions = list; drawStats(); });
    }

    private void updateSummary() {
        b.btnSettings.setText(String.format(Locale.getDefault(), "⚙  %d / %d / %d min  ·  Change",
                Prefs.focusMin(this), Prefs.shortBreakMin(this), Prefs.longBreakEvery(this)));
        updateCycleText();
    }

    private void updateCycleText() {
        Integer n = vm.completedFocus.getValue();
        b.cycleText.setText("Focus sessions completed: " + (n == null ? 0 : n)
                + "  (long break every " + Prefs.longBreakEvery(this) + ")");
    }

    /** Returns the value, or -1 (and flags the field) if it isn't a whole number in [min, max]. */
    private int readInt(EditText e, int min, int max) {
        try {
            int n = (int) Double.parseDouble(FormDialog.str(e));
            if (n >= min && n <= max) return n;
        } catch (NumberFormatException ignored) { }
        e.setError("Enter " + min + "-" + max);
        return -1;
    }

    private void showSettings() {
        FormDialog f = new FormDialog(this);
        Spinner preset = f.spinner("Preset", Arrays.asList(PRESET_NAMES), 0);
        EditText focus = f.number("Focus (minutes)", String.valueOf(Prefs.focusMin(this)));
        EditText sb = f.number("Short break (minutes)", String.valueOf(Prefs.shortBreakMin(this)));
        EditText lb = f.number("Long break (minutes)", String.valueOf(Prefs.longBreakEvery(this)));
        EditText every = f.number("Long break after how many focus sessions", String.valueOf(Prefs.longBreakEvery(this)));

        preset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos <= 0) return; // "Custom": leave the fields alone
                int[] p = PRESETS[pos];
                focus.setText(String.valueOf(p[0]));
                sb.setText(String.valueOf(p[1]));
                lb.setText(String.valueOf(p[2]));
                every.setText(String.valueOf(p[3]));
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        f.show("Timer settings", "Save", () -> {
            int a = readInt(focus, 1, 180);
            int s = readInt(sb, 1, 60);
            int l = readInt(lb, 1, 120);
            int e = readInt(every, 1, 12);
            if (a < 0 || s < 0 || l < 0 || e < 0) return false;

            Prefs.putInt(this, "focus_min", a);
            Prefs.putInt(this, "short_min", s);
            Prefs.putInt(this, "long_min", l);
            Prefs.putInt(this, "long_every", e);
            boolean wasRunning = Boolean.TRUE.equals(vm.running.getValue());
            vm.applySettings();
            updateSummary();
            UiUtil.toast(this, wasRunning ? "Saved - applies from the next phase" : "Timer updated");
            return true;
        });
    }

    private void drawStats() {
        long since = rangeIndex == 0 ? System.currentTimeMillis() - 7L * 86400000L : 0;
        Map<Long, Integer> seconds = new HashMap<>();
        for (StudySession s : sessions) {
            if (s.startMillis >= since) seconds.merge(s.courseId, s.seconds, Integer::sum);
        }
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : seconds.entrySet()) {
            Course c = null;
            for (Course x : courses) if (x.id == e.getKey()) c = x;
            labels.add(c == null ? "Other" : (c.code == null || c.code.isEmpty() ? c.name : c.code));
            values.add(e.getValue() / 60f);
            colors.add(c == null ? 0xFF9E9E9E : c.color);
        }
        b.barChart.setData(labels, values, colors, "m");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_SETTINGS, "Timer settings");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_SETTINGS) {
            showSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}