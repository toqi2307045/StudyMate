package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "study_sessions")
public class StudySession {
    @PrimaryKey(autoGenerate = true) public long id;
    public long courseId;
    public long startMillis;
    public int seconds;
}
