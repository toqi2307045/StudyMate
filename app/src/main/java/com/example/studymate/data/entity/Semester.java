package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "semesters")
public class Semester {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
}
