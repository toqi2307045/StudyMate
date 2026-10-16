package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.CourseFile;
import com.example.studymate.data.entity.RecentFile;

import java.util.List;

/** Shared by the Course Files screen and the PDF library (recent files). */
public class FilesViewModel extends AndroidViewModel {
    private final Repository repo;
    public final MutableLiveData<Long> courseId = new MutableLiveData<>(0L);
    public final LiveData<List<CourseFile>> files;
    public final LiveData<List<Course>> courses;
    public final LiveData<List<RecentFile>> recent;

    public FilesViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        courses = repo.courses();
        recent = repo.recentFiles();
        files = Transformations.switchMap(courseId, repo::filesForCourse);
    }

    public void addFile(CourseFile f) { repo.addCourseFile(f); }

    public void deleteFile(CourseFile f) { repo.deleteCourseFile(f); }

    public void deleteRecent(RecentFile r) { repo.deleteRecent(r); }
}
