package com.example.studymate.ui;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.Assignment;
import com.example.studymate.data.entity.Course;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.util.TimeUtil;
import com.example.studymate.viewmodel.AssignmentViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentsActivity extends BaseActivity {
    private static final int[] PRIORITY_COLORS = {0xFF4CAF50, 0xFFFF9800, 0xFFF44336};
    private static final String[] PRIORITY_NAMES = {"Low", "Medium", "High"};

    private ActivityListBinding b;
    private AssignmentViewModel vm;
    private final Map<Long, Course> courseMap = new HashMap<>();
    private List<Course> courses = new ArrayList<>();
    private RowAdapter<Assignment> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Assignments & To-do", true);
        vm = new ViewModelProvider(this).get(AssignmentViewModel.class);

        int initial = vm.filter.getValue() == null ? 1 : vm.filter.getValue();
        b.headerContainer.addView(UiUtil.segmented(this, new String[]{"All", "Pending", "Done"}, initial,
                i -> vm.filter.setValue(i)));

        adapter = new RowAdapter<>((r, a) -> {
            r.strip.setBackgroundColor(PRIORITY_COLORS[Math.max(0, Math.min(2, a.priority))]);
            r.title.setText(a.title);
            r.check.setVisibility(View.VISIBLE);
            r.check.setChecked(a.done);
            r.check.setOnCheckedChangeListener((cb, checked) -> vm.setDone(a, checked));
            if (a.done) {
                r.title.setPaintFlags(r.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                r.title.setAlpha(0.5f);
            }
            Course c = courseMap.get(a.courseId);
            StringBuilder sub = new StringBuilder();
            if (c != null) sub.append(c.label());
            boolean overdue = !a.done && a.dueMillis != 0 && a.dueMillis < System.currentTimeMillis();
            if (a.dueMillis != 0) {
                if (sub.length() > 0) sub.append("  ·  ");
                sub.append(overdue ? "Overdue: " : "Due ").append(TimeUtil.formatDateTime(a.dueMillis));
            }
            if (sub.length() == 0) sub.append("No due date");
            r.subtitle.setText(sub);
            r.subtitle.setTextColor(overdue ? 0xFFD32F2F : r.title.getCurrentTextColor());
            r.subtitle.setAlpha(overdue ? 1f : 0.75f);
            r.trailing.setText(PRIORITY_NAMES[Math.max(0, Math.min(2, a.priority))]);
        });
        adapter.onClick(this::showForm);
        adapter.onLongClick(a -> UiUtil.confirm(this, "Delete task?", a.title, "Delete", () -> vm.delete(a)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);

        vm.courses.observe(this, list -> {
            courses = list;
            courseMap.clear();
            for (Course c : list) courseMap.put(c.id, c);
            adapter.submit(vm.visible.getValue());
        });
        vm.visible.observe(this, list -> {
            adapter.submit(list);
            int f = vm.filter.getValue() == null ? 1 : vm.filter.getValue();
            b.empty.setText(f == 2 ? "Nothing completed yet." : "All caught up! 🎉\nTap + to add a task.");
            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        b.fab.setOnClickListener(v -> showForm(null));
    }

    private void showForm(Assignment existing) {
        FormDialog f = new FormDialog(this);
        EditText title = f.text("Title", existing == null ? "" : existing.title);
        EditText notes = f.multiline("Notes (optional)", existing == null ? "" : existing.notes);
        FormDialog.CourseField course = f.courseSpinner("Course", courses, existing == null ? 0 : existing.courseId, true);
        Spinner priority = f.spinner("Priority", Arrays.asList(PRIORITY_NAMES), existing == null ? 1 : existing.priority);
        FormDialog.DateField due = f.dateTime("Due date", existing == null ? 0 : existing.dueMillis, true, true);
        f.show(existing == null ? "New task" : "Edit task", "Save", () -> {
            if (FormDialog.str(title).isEmpty()) { title.setError("Required"); return false; }
            Assignment a = existing == null ? new Assignment() : existing;
            a.title = FormDialog.str(title);
            a.notes = FormDialog.str(notes);
            a.courseId = course.selectedId();
            a.priority = priority.getSelectedItemPosition();
            a.dueMillis = due.millis;
            vm.save(a);
            return true;
        });
    }
}
