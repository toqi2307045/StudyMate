package com.example.studymate.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.studymate.data.dao.AssignmentDao;
import com.example.studymate.data.dao.AttendanceDao;
import com.example.studymate.data.dao.BookmarkDao;
import com.example.studymate.data.dao.CourseDao;
import com.example.studymate.data.dao.CourseFileDao;
import com.example.studymate.data.dao.ExamDao;
import com.example.studymate.data.dao.GradeDao;
import com.example.studymate.data.dao.NoteDao;
import com.example.studymate.data.dao.RecentFileDao;
import com.example.studymate.data.dao.SemesterDao;
import com.example.studymate.data.dao.SessionDao;
import com.example.studymate.data.dao.StudyDao;
import com.example.studymate.data.entity.Assignment;
import com.example.studymate.data.entity.AttendanceRecord;
import com.example.studymate.data.entity.Bookmark;
import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.CourseFile;
import com.example.studymate.data.entity.Exam;
import com.example.studymate.data.entity.GradeEntry;
import com.example.studymate.data.entity.Note;
import com.example.studymate.data.entity.RecentFile;
import com.example.studymate.data.entity.Semester;
import com.example.studymate.data.entity.StudySession;

@Database(entities = {
        Course.class, ClassSession.class, AttendanceRecord.class, Semester.class,
        GradeEntry.class, Assignment.class, Exam.class, Note.class, StudySession.class,
        CourseFile.class, RecentFile.class, Bookmark.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CourseDao courseDao();
    public abstract SessionDao sessionDao();
    public abstract AttendanceDao attendanceDao();
    public abstract SemesterDao semesterDao();
    public abstract GradeDao gradeDao();
    public abstract AssignmentDao assignmentDao();
    public abstract ExamDao examDao();
    public abstract NoteDao noteDao();
    public abstract StudyDao studyDao();
    public abstract CourseFileDao courseFileDao();
    public abstract RecentFileDao recentFileDao();
    public abstract BookmarkDao bookmarkDao();

    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "StudyMate.db").build();
                }
            }
        }
        return instance;
    }
}
