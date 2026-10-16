package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grades")
public class GradeEntry {
    @PrimaryKey(autoGenerate = true) public long id;
    public long semesterId;
    public String courseName;
    public double credit;
    public String gradeLetter;
    public double gradePoint;
}
