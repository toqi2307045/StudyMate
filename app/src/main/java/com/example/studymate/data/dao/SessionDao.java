package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.ClassSession;

import java.util.List;

@Dao
public interface SessionDao extends BaseDao<ClassSession> {
    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :day ORDER BY startMin")
    LiveData<List<ClassSession>> observeDay(int day);

    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :day ORDER BY startMin")
    List<ClassSession> forDay(int day);

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek, startMin")
    List<ClassSession> all();

    @Query("SELECT * FROM class_sessions WHERE courseId = :courseId")
    List<ClassSession> forCourse(long courseId);

    @Query("SELECT * FROM class_sessions WHERE id = :id")
    ClassSession get(long id);

    @Query("DELETE FROM class_sessions WHERE courseId = :courseId")
    void deleteByCourse(long courseId);

    @Query("DELETE FROM class_sessions")
    void clear();
}
