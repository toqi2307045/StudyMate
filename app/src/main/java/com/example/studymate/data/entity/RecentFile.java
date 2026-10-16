package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "recent_files", indices = {@Index(value = {"uri"}, unique = true)})
public class RecentFile {
    @PrimaryKey(autoGenerate = true) public long id;
    public String uri;
    public String name;
    public int lastPage;
    public int pageCount;
    public long lastOpened;
}
