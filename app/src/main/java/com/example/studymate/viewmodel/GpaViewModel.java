package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.GradeEntry;
import com.example.studymate.data.entity.Semester;

import java.util.ArrayList;
import java.util.List;

public class GpaViewModel extends AndroidViewModel {

    public static class Summary {
        public List<Semester> semesters = new ArrayList<>();
        public Semester selected;
        public List<GradeEntry> selectedGrades = new ArrayList<>();
        public double semesterGpa, cgpa, semesterCredits, totalCredits;
        /** One value per semester, in order (for the progress chart). */
        public List<String> labels = new ArrayList<>();
        public List<Double> gpas = new ArrayList<>();
        public List<Double> cgpas = new ArrayList<>();
    }

    private final Repository repo;
    private final LiveData<List<Semester>> semesters;
    private final LiveData<List<GradeEntry>> grades;
    public final MutableLiveData<Long> selectedId = new MutableLiveData<>(0L);
    public final MediatorLiveData<Summary> summary = new MediatorLiveData<>();

    public GpaViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        semesters = repo.semesters();
        grades = repo.grades();
        summary.addSource(semesters, s -> recompute());
        summary.addSource(grades, g -> recompute());
        summary.addSource(selectedId, i -> recompute());
    }

    private static double gpa(List<GradeEntry> list) {
        double pts = 0, cr = 0;
        for (GradeEntry g : list) { pts += g.credit * g.gradePoint; cr += g.credit; }
        return cr == 0 ? 0 : pts / cr;
    }

    private void recompute() {
        List<Semester> ss = semesters.getValue();
        List<GradeEntry> gs = grades.getValue();
        if (ss == null || gs == null) return;
        Summary r = new Summary();
        r.semesters = ss;
        long sel = selectedId.getValue() == null ? 0 : selectedId.getValue();
        List<GradeEntry> running = new ArrayList<>();
        for (Semester s : ss) {
            List<GradeEntry> mine = new ArrayList<>();
            for (GradeEntry g : gs) if (g.semesterId == s.id) mine.add(g);
            running.addAll(mine);
            r.labels.add(s.name);
            r.gpas.add(gpa(mine));
            r.cgpas.add(gpa(running));
            if (s.id == sel) {
                r.selected = s;
                r.selectedGrades = mine;
                r.semesterGpa = gpa(mine);
                for (GradeEntry g : mine) r.semesterCredits += g.credit;
            }
        }
        if (r.selected == null && !ss.isEmpty()) {
            // fall back to the last semester
            Semester s = ss.get(ss.size() - 1);
            r.selected = s;
            for (GradeEntry g : gs) if (g.semesterId == s.id) r.selectedGrades.add(g);
            r.semesterGpa = gpa(r.selectedGrades);
            for (GradeEntry g : r.selectedGrades) r.semesterCredits += g.credit;
        }
        r.cgpa = gpa(running);
        for (GradeEntry g : running) r.totalCredits += g.credit;
        summary.setValue(r);
    }

    public void addSemester(String name) {
        repo.addSemester(name, id -> selectedId.setValue(id));
    }

    public void deleteSemester(Semester s) { repo.deleteSemester(s); }

    public void renameSemester(Semester s) { repo.updateSemester(s); }

    public void saveGrade(GradeEntry g) { repo.saveGrade(g); }

    public void deleteGrade(GradeEntry g) { repo.deleteGrade(g); }
}
