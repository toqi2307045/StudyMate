package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class Bookmark {
    @PrimaryKey(autoGenerate = true) public long id;
    public String fileUri;
    public int page;
    public long createdAt;
}
