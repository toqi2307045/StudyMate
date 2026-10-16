package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.Course;

import java.util.List;

@Dao
public interface CourseDao extends BaseDao<Course> {
    @Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE")
    LiveData<List<Course>> observeAll();

    @Query("SELECT * FROM courses ORDER BY id")
    List<Course> all();

    @Query("SELECT * FROM courses WHERE id = :id")
    Course get(long id);

    @Query("DELETE FROM courses WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM courses")
    void clear();
}
