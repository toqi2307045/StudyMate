package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.Note;

import java.util.List;

@Dao
public interface NoteDao extends BaseDao<Note> {
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' "
            + "OR tags LIKE '%' || :q || '%' ORDER BY updatedAt DESC")
    LiveData<List<Note>> search(String q);

    @Query("SELECT * FROM notes ORDER BY id")
    List<Note> all();

    @Query("SELECT * FROM notes WHERE id = :id")
    Note get(long id);

    @Query("UPDATE notes SET courseId = 0 WHERE courseId = :courseId")
    void unlinkCourse(long courseId);

    @Query("DELETE FROM notes")
    void clear();
}
