package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.AttendanceRecord;

import java.util.List;

@Dao
public interface AttendanceDao extends BaseDao<AttendanceRecord> {
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    LiveData<List<AttendanceRecord>> observeAll();

    @Query("SELECT * FROM attendance ORDER BY id")
    List<AttendanceRecord> all();

    @Query("SELECT * FROM attendance WHERE courseId = :courseId ORDER BY date DESC")
    List<AttendanceRecord> forCourse(long courseId);

    @Query("DELETE FROM attendance WHERE courseId = :courseId")
    void deleteByCourse(long courseId);

    @Query("DELETE FROM attendance")
    void clear();
}
