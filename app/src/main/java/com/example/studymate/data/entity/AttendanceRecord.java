package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** status: 0 = present, 1 = absent, 2 = late. date = epoch millis at local start of day. */
@Entity(tableName = "attendance",
        indices = {@Index(value = {"courseId", "date"}, unique = true)})
public class AttendanceRecord {
    public static final int PRESENT = 0, ABSENT = 1, LATE = 2;

    @PrimaryKey(autoGenerate = true) public long id;
    public long courseId;
    public long date;
    public int status;
}
