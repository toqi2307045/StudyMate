package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** type: pdf | pptx | docx. category: Lecture | Past paper | Other. */
@Entity(tableName = "course_files")
public class CourseFile {
    @PrimaryKey(autoGenerate = true) public long id;
    public long courseId;
    public String name;
    public String uri;
    public String type;
    public String category;
    public long addedAt;
}
