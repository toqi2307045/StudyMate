package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true) public long id;
    public String title;
    public String body;
    public String tags;
    public long courseId;
    public long updatedAt;
}
