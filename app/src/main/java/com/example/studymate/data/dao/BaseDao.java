package com.example.studymate.data.dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import java.util.List;

public interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(T item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<T> items);

    @Update
    void update(T item);

    @Delete
    void delete(T item);
}
