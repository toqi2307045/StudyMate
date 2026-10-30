package com.example.studymate.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Exam;
import com.example.studymate.databinding.ActivityMainBinding;
import com.example.studymate.databinding.ItemTileBinding;
import com.example.studymate.util.TimeUtil;
import com.example.studymate.viewmodel.ExamViewModel;
import com.example.studymate.viewmodel.RoutineViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private static class Tile {
        final String icon, label;
        final Class<?> target;

        Tile(String icon, String label, Class<?> target) {
            this.icon = icon; this.label = label; this.target = target;
        }
    }

    private ActivityMainBinding b;
    private final Map<Long, Course> courseMap = new HashMap<>();
    private List<Exam> exams;
    private RowAdapter<ClassSession> todayAdapter;

    private final androidx.activity.result.ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "StudyMate", false);

        if (!com.example.studymate.firebase.FirebaseSync.isSignedIn()
                && !com.example.studymate.util.Prefs.getBool(this, "skip_login_prompt", false)
                && savedInstanceState == null) {
            startActivity(new Intent(this, LoginActivity.class));
        }

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        b.todayTitle.setText("Today · " + TimeUtil.dayName(TimeUtil.todayIso()));

        todayAdapter = new RowAdapter<>((r, s) -> {
            Course c = courseMap.get(s.courseId);
            r.strip.setBackgroundColor(c == null ? 0xFF9E9E9E : c.color);
            r.title.setText(c == null ? "Class" : c.label());
            r.subtitle.setText(TimeUtil.hm(s.startMin) + " – " + TimeUtil.hm(s.endMin)
                    + (s.room == null || s.room.isEmpty() ? "" : " · Room " + s.room));
        });
        todayAdapter.onClick(s -> startActivity(new Intent(this, RoutineActivity.class)));
        b.todayList.setLayoutManager(new LinearLayoutManager(this));
        b.todayList.setAdapter(todayAdapter);

        RoutineViewModel rvm = new ViewModelProvider(this).get(RoutineViewModel.class);
        rvm.day.setValue(TimeUtil.todayIso());
        rvm.courses.observe(this, list -> {
            courseMap.clear();
            for (Course c : list) courseMap.put(c.id, c);
            todayAdapter.refresh();
            updateNextExam();
        });
        rvm.sessions.observe(this, list -> {
            todayAdapter.submit(list);
            b.todayEmpty.setVisibility(list.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        ExamViewModel evm = new ViewModelProvider(this).get(ExamViewModel.class);
        evm.exams.observe(this, list -> { exams = list; updateNextExam(); });

        Tile[] tiles = {
                new Tile("📅", "Class Routine", RoutineActivity.class),
                new Tile("✅", "Attendance", AttendanceActivity.class),
                new Tile("🎓", "GPA / CGPA", GpaActivity.class),
                new Tile("📝", "Assignments", AssignmentsActivity.class),
                new Tile("⏳", "Exam Countdown", ExamsActivity.class),
                new Tile("🗒️", "Notes", NotesActivity.class),
                new Tile("🍅", "Pomodoro", PomodoroActivity.class),
                new Tile("📚", "Courses", CoursesActivity.class),
                new Tile("📁", "Course Files", CourseFilesActivity.class),
                new Tile("📄", "PDF Reader", PdfLibraryActivity.class),
                new Tile("🛠️", "Document Tools", DocumentToolsActivity.class),
                new Tile("⚙️", "Settings & Backup", SettingsActivity.class),
        };
        b.tiles.setLayoutManager(new GridLayoutManager(this, 2));
        b.tiles.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ItemTileBinding tb = ItemTileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new RecyclerView.ViewHolder(tb.getRoot()) { };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
                ItemTileBinding tb = ItemTileBinding.bind(h.itemView);
                Tile t = tiles[position];
                tb.tileIcon.setText(t.icon);
                tb.tileLabel.setText(t.label);
                tb.tileCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, t.target)));
            }

            @Override
            public int getItemCount() { return tiles.length; }
        });
    }

    private void updateNextExam() {
        if (exams == null) return;
        long today = TimeUtil.startOfDay(System.currentTimeMillis());
        for (Exam e : exams) {
            if (e.dateMillis >= today) {
                long d = TimeUtil.daysUntil(e.dateMillis);
                Course c = courseMap.get(e.courseId);
                b.nextExam.setText("⏳ Next exam: " + (c == null ? "" : c.label() + " – ") + e.title
                        + (d == 0 ? " is today" : d == 1 ? " is tomorrow" : " in " + d + " days"));
                return;
            }
        }
        b.nextExam.setText("");
    }
}
