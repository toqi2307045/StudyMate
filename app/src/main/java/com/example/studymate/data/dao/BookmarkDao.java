package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.Bookmark;

import java.util.List;

@Dao
public interface BookmarkDao extends BaseDao<Bookmark> {
    @Query("SELECT * FROM bookmarks WHERE fileUri = :uri ORDER BY page")
    List<Bookmark> forFile(String uri);
}
