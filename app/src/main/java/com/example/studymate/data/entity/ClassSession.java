package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One weekly class slot. dayOfWeek is ISO (1 = Monday ... 7 = Sunday). */
@Entity(tableName = "class_sessions")
public class ClassSession {
    @PrimaryKey(autoGenerate = true) public long id;
    public long courseId;
    public int dayOfWeek;
    public int startMin;
    public int endMin;
    public String room;
}
