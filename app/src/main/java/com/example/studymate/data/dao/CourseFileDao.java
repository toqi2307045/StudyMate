package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.CourseFile;

import java.util.List;

@Dao
public interface CourseFileDao extends BaseDao<CourseFile> {
    @Query("SELECT * FROM course_files WHERE courseId = :courseId ORDER BY addedAt DESC")
    LiveData<List<CourseFile>> observeForCourse(long courseId);

    @Query("DELETE FROM course_files WHERE courseId = :courseId")
    void deleteByCourse(long courseId);
}
