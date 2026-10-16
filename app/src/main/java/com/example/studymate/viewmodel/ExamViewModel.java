package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Exam;

import java.util.List;

public class ExamViewModel extends AndroidViewModel {
    private final Repository repo;
    public final LiveData<List<Exam>> exams;
    public final LiveData<List<Course>> courses;

    public ExamViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        exams = repo.exams();
        courses = repo.courses();
    }

    public void save(Exam e) { repo.saveExam(e); }

    public void delete(Exam e) { repo.deleteExam(e); }
}
