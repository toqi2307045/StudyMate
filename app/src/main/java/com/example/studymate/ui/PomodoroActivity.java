package com.example.studymate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.lifecycle.ViewModelProvider;

import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.StudySession;
import com.example.studymate.databinding.ActivityPomodoroBinding;
import com.example.studymate.util.Prefs;
import com.example.studymate.viewmodel.PomodoroViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PomodoroActivity extends BaseActivity {
    private static final int M_SETTINGS = 1;

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

        vm.remainingMs.observe(this, ms -> {
            long s = (ms + 999) / 1000;
            b.timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60));
        });
        vm.phase.observe(this, p -> {
            b.phaseLabel.setText(p == PomodoroViewModel.FOCUS ? "🎯 Focus" : p == PomodoroViewModel.SHORT_BREAK ? "☕ Short break" : "🌿 Long break");
            b.courseSpinner.setEnabled(p == PomodoroViewModel.FOCUS);
        });
        vm.completedFocus.observe(this, n -> b.cycleText.setText("Focus sessions completed: " + n + "  (long break every 4)"));
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
        addMenu(menu, M_SETTINGS, "Timer durations");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != M_SETTINGS) return super.onOptionsItemSelected(item);
        FormDialog f = new FormDialog(this);
        EditText focus = f.number("Focus (minutes)", String.valueOf(Prefs.focusMin(this)));
        EditText sb = f.number("Short break (minutes)", String.valueOf(Prefs.shortBreakMin(this)));
        EditText lb = f.number("Long break (minutes)", String.valueOf(Prefs.longBreakMin(this)));
        f.show("Timer durations", "Save", () -> {
            try {
                int a = (int) Double.parseDouble(FormDialog.str(focus));
                int s = (int) Double.parseDouble(FormDialog.str(sb));
                int l = (int) Double.parseDouble(FormDialog.str(lb));
                if (a < 1 || s < 1 || l < 1) throw new NumberFormatException();
                Prefs.putInt(this, "focus_min", a);
                Prefs.putInt(this, "short_min", s);
                Prefs.putInt(this, "long_min", l);
                if (!Boolean.TRUE.equals(vm.running.getValue())) vm.reset();
                return true;
            } catch (NumberFormatException e) {
                UiUtil.toast(this, "Enter whole minutes (1 or more)");
                return false;
            }
        });
        return true;
    }
}
