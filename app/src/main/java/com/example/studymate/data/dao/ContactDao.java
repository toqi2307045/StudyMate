package com.example.studymate.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.studymate.data.entity.Contact;

import java.util.List;

@Dao
public interface ContactDao extends BaseDao<Contact> {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE")
    LiveData<List<Contact>> observeAll();

    @Query("SELECT * FROM contacts ORDER BY id")
    List<Contact> all();

    @Query("SELECT * FROM contacts WHERE id = :id")
    Contact get(long id);
}
