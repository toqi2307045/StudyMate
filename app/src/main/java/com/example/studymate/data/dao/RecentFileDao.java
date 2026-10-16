package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.RecentFile;

import java.util.List;

@Dao
public interface RecentFileDao extends BaseDao<RecentFile> {
    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC LIMIT 30")
    LiveData<List<RecentFile>> observeRecent();

    @Query("SELECT * FROM recent_files WHERE uri = :uri LIMIT 1")
    RecentFile find(String uri);
}
