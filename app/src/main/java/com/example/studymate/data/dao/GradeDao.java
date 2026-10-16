package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.GradeEntry;

import java.util.List;

@Dao
public interface GradeDao extends BaseDao<GradeEntry> {
    @Query("SELECT * FROM grades ORDER BY id")
    LiveData<List<GradeEntry>> observeAll();

    @Query("SELECT * FROM grades ORDER BY id")
    List<GradeEntry> all();

    @Query("DELETE FROM grades WHERE semesterId = :semesterId")
    void deleteBySemester(long semesterId);

    @Query("DELETE FROM grades")
    void clear();
}
