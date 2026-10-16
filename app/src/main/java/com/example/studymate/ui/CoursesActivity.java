package com.example.studymate.ui;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.Course;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.viewmodel.CourseViewModel;

public class CoursesActivity extends BaseActivity {
    private ActivityListBinding b;
    private CourseViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Courses", true);
        vm = new ViewModelProvider(this).get(CourseViewModel.class);

        RowAdapter<Course> adapter = new RowAdapter<>((r, c) -> {
            r.strip.setBackgroundColor(c.color);
            r.title.setText(c.name);
            String sub = (c.code == null ? "" : c.code)
                    + (c.teacher == null || c.teacher.isEmpty() ? "" : " · " + c.teacher);
            r.subtitle.setText(sub);
        });
        adapter.onClick(this::showForm);
        adapter.onLongClick(c -> UiUtil.confirm(this, "Delete course?",
                "This also deletes its classes, attendance and study sessions. Tasks, exams and notes are kept but unlinked.",
                "Delete", () -> vm.delete(c)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No courses yet.\nTap + to add your first course.");

        vm.courses.observe(this, list -> {
            adapter.submit(list);
            b.empty.setVisibility(list.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        });
        b.fab.setOnClickListener(v -> showForm(null));
    }

    private void showForm(Course existing) {
        FormDialog f = new FormDialog(this);
        android.widget.EditText name = f.text("Course name", existing == null ? "" : existing.name);
        android.widget.EditText code = f.text("Course code (e.g. CSE 2101)", existing == null ? "" : existing.code);
        android.widget.EditText teacher = f.text("Teacher (optional)", existing == null ? "" : existing.teacher);
        FormDialog.ColorField color = f.color(existing == null ? 0 : existing.color);
        f.show(existing == null ? "New course" : "Edit course", "Save", () -> {
            if (FormDialog.str(name).isEmpty()) { name.setError("Required"); return false; }
            Course c = existing == null ? new Course() : existing;
            c.name = FormDialog.str(name);
            c.code = FormDialog.str(code);
            c.teacher = FormDialog.str(teacher);
            c.color = color.color;
            vm.save(c);
            return true;
        });
    }
}
