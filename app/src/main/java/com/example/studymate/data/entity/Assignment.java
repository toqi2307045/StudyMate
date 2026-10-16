package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** priority: 0 = low, 1 = medium, 2 = high. dueMillis == 0 means no due date. */
@Entity(tableName = "assignments")
public class Assignment {
    @PrimaryKey(autoGenerate = true) public long id;
    public String title;
    public String notes;
    public long courseId;
    public int priority;
    public long dueMillis;
    public boolean done;
}
