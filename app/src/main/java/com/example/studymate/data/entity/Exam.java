package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "exams")
public class Exam {
    @PrimaryKey(autoGenerate = true) public long id;
    public long courseId;
    public String title;
    public long dateMillis;
    public String room;
    public String syllabus;
}
