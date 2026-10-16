package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "courses")
public class Course {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String code;
    public String teacher;
    public int color;

    public String label() {
        return (code == null || code.trim().isEmpty()) ? name : code + " · " + name;
    }
}
