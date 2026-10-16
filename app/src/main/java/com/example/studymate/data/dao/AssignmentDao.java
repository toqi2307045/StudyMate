package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.Assignment;

import java.util.List;

@Dao
public interface AssignmentDao extends BaseDao<Assignment> {
    @Query("SELECT * FROM assignments ORDER BY done, CASE WHEN dueMillis = 0 THEN 1 ELSE 0 END, dueMillis")
    LiveData<List<Assignment>> observeAll();

    @Query("SELECT * FROM assignments ORDER BY id")
    List<Assignment> all();

    @Query("SELECT * FROM assignments WHERE id = :id")
    Assignment get(long id);

    @Query("UPDATE assignments SET courseId = 0 WHERE courseId = :courseId")
    void unlinkCourse(long courseId);

    @Query("DELETE FROM assignments")
    void clear();
}
