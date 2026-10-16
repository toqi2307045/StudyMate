package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.StudySession;

import java.util.List;

@Dao
public interface StudyDao extends BaseDao<StudySession> {
    @Query("SELECT * FROM study_sessions ORDER BY startMillis")
    LiveData<List<StudySession>> observeAll();

    @Query("SELECT * FROM study_sessions ORDER BY id")
    List<StudySession> all();

    @Query("DELETE FROM study_sessions WHERE courseId = :courseId")
    void deleteByCourse(long courseId);

    @Query("DELETE FROM study_sessions")
    void clear();
}
