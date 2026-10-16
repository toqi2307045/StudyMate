package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.util.TimeUtil;

import java.util.List;

public class RoutineViewModel extends AndroidViewModel {
    private final Repository repo;
    public final MutableLiveData<Integer> day = new MutableLiveData<>(TimeUtil.todayIso());
    public final LiveData<List<ClassSession>> sessions;
    public final LiveData<List<Course>> courses;

    public RoutineViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        courses = repo.courses();
        sessions = Transformations.switchMap(day, repo::sessionsForDay);
    }

    public void save(ClassSession s) { repo.saveSession(s); }

    public void delete(ClassSession s) { repo.deleteSession(s); }

    public void exportText(Repository.Callback<String> cb) { repo.exportRoutineText(cb); }

    public void importText(String text, Repository.Callback<Integer> cb) { repo.importRoutineText(text, cb); }
}
