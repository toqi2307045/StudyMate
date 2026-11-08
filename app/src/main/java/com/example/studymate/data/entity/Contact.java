package com.example.studymate.data.entity;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A teacher or classmate. type: 0 = teacher, 1 = classmate.
 * syncId is a UUID that stays the same on every device (used as the Firestore document id).
 * photoPath is a local file; only a small thumbnail travels to Firestore.
 */
@Entity(tableName = "contacts", indices = {@Index(value = {"syncId"}, unique = true)})
public class Contact {
    public static final int TEACHER = 0, CLASSMATE = 1;

    @PrimaryKey(autoGenerate = true) public long id;
    public String syncId;
    public int type;
    public String name;
    public String phone;
    public String email;
    public String rollOrDesignation;
    public String photoPath;
    public long updatedAt;
}
