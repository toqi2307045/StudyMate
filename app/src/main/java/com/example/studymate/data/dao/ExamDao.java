package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.Exam;

import java.util.List;

@Dao
public interface ExamDao extends BaseDao<Exam> {
    @Query("SELECT * FROM exams ORDER BY dateMillis")
    LiveData<List<Exam>> observeAll();

    @Query("SELECT * FROM exams ORDER BY id")
    List<Exam> all();

    @Query("UPDATE exams SET courseId = 0 WHERE courseId = :courseId")
    void unlinkCourse(long courseId);

    @Query("DELETE FROM exams")
    void clear();
}
