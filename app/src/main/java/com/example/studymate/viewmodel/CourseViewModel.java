package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Course;

import java.util.List;

public class CourseViewModel extends AndroidViewModel {
    private final Repository repo;
    public final LiveData<List<Course>> courses;

    public CourseViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        courses = repo.courses();
    }

    public void save(Course c) { repo.saveCourse(c); }

    public void delete(Course c) { repo.deleteCourse(c); }
}
