package com.example.studymate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.GradeEntry;
import com.example.studymate.data.entity.Semester;
import com.example.studymate.databinding.ActivityGpaBinding;
import com.example.studymate.util.Grades;
import com.example.studymate.viewmodel.GpaViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GpaActivity extends BaseActivity {
    private static final int M_DELETE_SEM = 1, M_RENAME = 2;

    private ActivityGpaBinding b;
    private GpaViewModel vm;
    private GpaViewModel.Summary current;
    private RowAdapter<GradeEntry> adapter;
    private boolean updatingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGpaBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "GPA / CGPA", true);
        vm = new ViewModelProvider(this).get(GpaViewModel.class);

        adapter = new RowAdapter<>((r, g) -> {
            r.strip.setBackgroundColor(0xFF3F51B5);
            r.title.setText(g.courseName);
            r.subtitle.setText(String.format(Locale.getDefault(), "%.2f credits · grade point %.2f", g.credit, g.gradePoint));
            r.trailing.setText(g.gradeLetter);
        });
        adapter.onClick(this::showGradeForm);
        adapter.onLongClick(g -> UiUtil.confirm(this, "Delete entry?", g.courseName, "Delete", () -> vm.deleteGrade(g)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);

        b.semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (updatingSpinner || current == null || pos >= current.semesters.size()) return;
                long semId = current.semesters.get(pos).id;
                if (current.selected == null || current.selected.id != semId) vm.selectedId.setValue(semId);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        vm.summary.observe(this, s -> {
            current = s;
            List<String> names = new ArrayList<>();
            int selIdx = 0;
            for (int i = 0; i < s.semesters.size(); i++) {
                names.add(s.semesters.get(i).name);
                if (s.selected != null && s.semesters.get(i).id == s.selected.id) selIdx = i;
            }
            updatingSpinner = true;
            ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            b.semesterSpinner.setAdapter(ad);
            if (!names.isEmpty()) b.semesterSpinner.setSelection(selIdx);
            updatingSpinner = false;

            b.gpaValue.setText(String.format(Locale.getDefault(), "%.2f", s.semesterGpa));
            b.gpaCredits.setText(String.format(Locale.getDefault(), "%.1f credits", s.semesterCredits));
            b.cgpaValue.setText(String.format(Locale.getDefault(), "%.2f", s.cgpa));
            b.cgpaCredits.setText(String.format(Locale.getDefault(), "%.1f credits total", s.totalCredits));
            adapter.submit(s.selectedGrades);

            List<List<Double>> series = new ArrayList<>();
            series.add(s.gpas);
            series.add(s.cgpas);
            b.chart.setData(s.labels, Arrays.asList("Semester GPA", "CGPA"), series, 4.0);
            invalidateOptionsMenu();
        });

        b.btnAddSemester.setOnClickListener(v -> showSemesterForm(null));
        b.fab.setOnClickListener(v -> showGradeForm(null));
    }

    private void showSemesterForm(Semester existing) {
        FormDialog f = new FormDialog(this);
        int n = current == null ? 1 : current.semesters.size() + 1;
        EditText name = f.text("Semester name", existing == null ? "Semester " + n : existing.name);
        f.show(existing == null ? "New semester" : "Rename semester", "Save", () -> {
            if (FormDialog.str(name).isEmpty()) { name.setError("Required"); return false; }
            if (existing == null) {
                vm.addSemester(FormDialog.str(name));
            } else {
                existing.name = FormDialog.str(name);
                vm.renameSemester(existing);
            }
            return true;
        });
    }

    private void showGradeForm(GradeEntry existing) {
        if (current == null || current.selected == null) {
            UiUtil.toast(this, "Add a semester first");
            showSemesterForm(null);
            return;
        }
        FormDialog f = new FormDialog(this);
        EditText name = f.text("Course name", existing == null ? "" : existing.courseName);
        EditText credit = f.number("Credit hours", existing == null ? "3" : trim(existing.credit));
        Spinner grade = f.spinner("Grade", Arrays.asList(Grades.LETTERS), existing == null ? 0 : Grades.indexOf(existing.gradeLetter));
        long semId = current.selected.id;
        f.show(existing == null ? "Add course grade" : "Edit course grade", "Save", () -> {
            if (FormDialog.str(name).isEmpty()) { name.setError("Required"); return false; }
            double cr;
            try {
                cr = Double.parseDouble(FormDialog.str(credit));
            } catch (NumberFormatException e) {
                credit.setError("Enter a number");
                return false;
            }
            if (cr <= 0) { credit.setError("Must be above 0"); return false; }
            GradeEntry g = existing == null ? new GradeEntry() : existing;
            g.semesterId = existing == null ? semId : existing.semesterId;
            g.courseName = FormDialog.str(name);
            g.credit = cr;
            g.gradeLetter = Grades.LETTERS[grade.getSelectedItemPosition()];
            g.gradePoint = Grades.POINTS[grade.getSelectedItemPosition()];
            vm.saveGrade(g);
            return true;
        });
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_RENAME, "Rename semester");
        addMenu(menu, M_DELETE_SEM, "Delete semester");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (current == null || current.selected == null) return super.onOptionsItemSelected(item);
        Semester sem = current.selected;
        if (item.getItemId() == M_RENAME) {
            showSemesterForm(sem);
            return true;
        } else if (item.getItemId() == M_DELETE_SEM) {
            UiUtil.confirm(this, "Delete semester?", "\"" + sem.name + "\" and all its grades will be removed.",
                    "Delete", () -> { vm.selectedId.setValue(0L); vm.deleteSemester(sem); });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
