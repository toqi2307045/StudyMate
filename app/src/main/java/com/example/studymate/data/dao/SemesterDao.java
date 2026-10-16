package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.Semester;

import java.util.List;

@Dao
public interface SemesterDao extends BaseDao<Semester> {
    @Query("SELECT * FROM semesters ORDER BY id")
    LiveData<List<Semester>> observeAll();

    @Query("SELECT * FROM semesters ORDER BY id")
    List<Semester> all();

    @Query("DELETE FROM semesters")
    void clear();
}
