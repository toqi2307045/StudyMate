package com.example.studymate.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Exam;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.util.TimeUtil;
import com.example.studymate.viewmodel.ExamViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamsActivity extends BaseActivity {
    private ActivityListBinding b;
    private ExamViewModel vm;
    private final Map<Long, Course> courseMap = new HashMap<>();
    private List<Course> courses = new ArrayList<>();
    private RowAdapter<Exam> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Exam Countdown", true);
        vm = new ViewModelProvider(this).get(ExamViewModel.class);

        adapter = new RowAdapter<>((r, e) -> {
            Course c = courseMap.get(e.courseId);
            r.strip.setBackgroundColor(c == null ? 0xFF9E9E9E : c.color);
            r.title.setText((c == null ? "" : c.label() + " – ") + e.title);
            r.subtitle.setText(TimeUtil.formatDateTime(e.dateMillis)
                    + (e.room == null || e.room.isEmpty() ? "" : " · Room " + e.room));
            long d = TimeUtil.daysUntil(e.dateMillis);
            if (d < 0) { r.trailing.setText("Done"); r.trailing.setTextColor(0xFF9E9E9E); }
            else if (d == 0) { r.trailing.setText("TODAY"); r.trailing.setTextColor(0xFFD32F2F); }
            else {
                r.trailing.setText(d + (d == 1 ? " day" : " days"));
                r.trailing.setTextColor(d <= 3 ? 0xFFD32F2F : d <= 7 ? 0xFFF57C00 : r.title.getCurrentTextColor());
            }
        });
        adapter.onClick(this::showDetails);
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No exams yet.\nTap + to add one and start the countdown.");

        vm.courses.observe(this, list -> {
            courses = list;
            courseMap.clear();
            for (Course c : list) courseMap.put(c.id, c);
            adapter.refresh();
        });
        vm.exams.observe(this, list -> {
            adapter.submit(list);
            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        b.fab.setOnClickListener(v -> showForm(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.refresh(); // day counts change overnight
    }

    private void showDetails(Exam e) {
        Course c = courseMap.get(e.courseId);
        long d = TimeUtil.daysUntil(e.dateMillis);
        String when = d < 0 ? "Finished" : d == 0 ? "Today!" : d + " day(s) remaining";
        String msg = when + "\n\n" + TimeUtil.formatDateTime(e.dateMillis)
                + (e.room == null || e.room.isEmpty() ? "" : "\nRoom: " + e.room)
                + "\n\nSyllabus:\n" + (e.syllabus == null || e.syllabus.isEmpty() ? "(not added)" : e.syllabus);
        new MaterialAlertDialogBuilder(this)
                .setTitle((c == null ? "" : c.label() + " – ") + e.title)
                .setMessage(msg)
                .setPositiveButton("Edit", (dlg, w) -> showForm(e))
                .setNeutralButton("Delete", (dlg, w) -> UiUtil.confirm(this, "Delete exam?", e.title, "Delete", () -> vm.delete(e)))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showForm(Exam existing) {
        FormDialog f = new FormDialog(this);
        FormDialog.CourseField course = f.courseSpinner("Course", courses, existing == null ? 0 : existing.courseId, true);
        EditText title = f.text("Exam title (e.g. Midterm)", existing == null ? "" : existing.title);
        long defaultDate = System.currentTimeMillis() + 7L * 86400000L;
        FormDialog.DateField date = f.dateTime("Date & time", existing == null ? defaultDate : existing.dateMillis, true, false);
        EditText room = f.text("Room", existing == null ? "" : existing.room);
        EditText syllabus = f.multiline("Syllabus / topics", existing == null ? "" : existing.syllabus);
        f.show(existing == null ? "New exam" : "Edit exam", "Save", () -> {
            if (FormDialog.str(title).isEmpty()) { title.setError("Required"); return false; }
            Exam e = existing == null ? new Exam() : existing;
            e.courseId = course.selectedId();
            e.title = FormDialog.str(title);
            e.dateMillis = date.millis;
            e.room = FormDialog.str(room);
            e.syllabus = FormDialog.str(syllabus);
            vm.save(e);
            return true;
        });
    }
}
