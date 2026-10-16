package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Assignment;
import com.example.studymate.data.entity.Course;

import java.util.ArrayList;
import java.util.List;

public class AssignmentViewModel extends AndroidViewModel {
    private final Repository repo;
    private final LiveData<List<Assignment>> all;
    /** 0 = all, 1 = pending, 2 = done. */
    public final MutableLiveData<Integer> filter = new MutableLiveData<>(1);
    public final MediatorLiveData<List<Assignment>> visible = new MediatorLiveData<>();
    public final LiveData<List<Course>> courses;

    public AssignmentViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        all = repo.assignments();
        courses = repo.courses();
        visible.addSource(all, a -> recompute());
        visible.addSource(filter, f -> recompute());
    }

    private void recompute() {
        List<Assignment> list = all.getValue();
        Integer f = filter.getValue();
        if (list == null || f == null) return;
        List<Assignment> out = new ArrayList<>();
        for (Assignment a : list) {
            if (f == 0 || (f == 1 && !a.done) || (f == 2 && a.done)) out.add(a);
        }
        visible.setValue(out);
    }

    public void save(Assignment a) { repo.saveAssignment(a); }

    public void delete(Assignment a) { repo.deleteAssignment(a); }

    public void setDone(Assignment a, boolean done) {
        a.done = done;
        repo.saveAssignment(a);
    }
}
