package com.example.studymate.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studymate.backup.BackupManager;
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
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.NotificationHelper;
import com.example.studymate.util.ReminderScheduler;
import com.example.studymate.util.RoutineShare;
import com.example.studymate.widget.RoutineWidgetProvider;
import com.example.studymate.util.GradeScale;
import java.util.List;
import java.util.Map;

/** Single access point for all data + the side effects (reminders, widget, notifications). */
public class Repository {
    public interface Callback<T> { void done(T result); }

    private static volatile Repository instance;

    private final Context app;
    private final AppDatabase db;

    private Repository(Context c) {
        app = c.getApplicationContext();
        db = AppDatabase.getInstance(app);
    }

    public static Repository get(Context c) {
        if (instance == null) {
            synchronized (Repository.class) {
                if (instance == null) instance = new Repository(c);
            }
        }
        return instance;
    }

    private static void io(Runnable r) { AppExecutors.io(r); }

    private <T> void post(Callback<T> cb, T value) {
        if (cb != null) AppExecutors.main(() -> cb.done(value));
    }

    // ---------------- courses ----------------
    public LiveData<List<Course>> courses() { return db.courseDao().observeAll(); }

    public void saveCourse(Course c) {
        io(() -> {
            if (c.id == 0) db.courseDao().insert(c); else db.courseDao().update(c);
            RoutineWidgetProvider.refresh(app);
        });
    }

    public void deleteCourse(Course c) {
        io(() -> {
            for (ClassSession s : db.sessionDao().forCourse(c.id)) ReminderScheduler.cancelClass(app, s.id);
            db.runInTransaction(() -> {
                db.sessionDao().deleteByCourse(c.id);
                db.attendanceDao().deleteByCourse(c.id);
                db.studyDao().deleteByCourse(c.id);
                db.courseFileDao().deleteByCourse(c.id);
                db.assignmentDao().unlinkCourse(c.id);
                db.examDao().unlinkCourse(c.id);
                db.noteDao().unlinkCourse(c.id);
                db.courseDao().delete(c);
            });
            RoutineWidgetProvider.refresh(app);
        });
    }

    // ---------------- routine ----------------
    public LiveData<List<ClassSession>> sessionsForDay(int day) { return db.sessionDao().observeDay(day); }

    public void saveSession(ClassSession s) {
        io(() -> {
            if (s.id == 0) s.id = db.sessionDao().insert(s); else db.sessionDao().update(s);
            ReminderScheduler.scheduleClass(app, s);
            RoutineWidgetProvider.refresh(app);
        });
    }

    public void deleteSession(ClassSession s) {
        io(() -> {
            ReminderScheduler.cancelClass(app, s.id);
            db.sessionDao().delete(s);
            RoutineWidgetProvider.refresh(app);
        });
    }

    public void exportRoutineText(Callback<String> cb) {
        io(() -> post(cb, RoutineShare.encode(db.courseDao().all(), db.sessionDao().all())));
    }

    /** Imports a routine produced by {@link RoutineShare#encode}; result = number of classes added or -1. */
    public void importRoutineText(String text, Callback<Integer> cb) {
        RoutineShare.Parsed p = RoutineShare.decode(text);
        if (p == null) { post(cb, -1); return; }
        io(() -> {
            long[] ids = new long[p.courses.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = db.courseDao().insert(p.courses.get(i));
            int added = 0;
            for (int i = 0; i < p.sessions.size(); i++) {
                int ci = p.sessionCourse.get(i);
                if (ci < 0 || ci >= ids.length) continue;
                ClassSession s = p.sessions.get(i);
                s.courseId = ids[ci];
                s.id = db.sessionDao().insert(s);
                ReminderScheduler.scheduleClass(app, s);
                added++;
            }
            RoutineWidgetProvider.refresh(app);
            post(cb, added);
        });
    }

    // ---------------- attendance ----------------
    public LiveData<List<AttendanceRecord>> attendance() { return db.attendanceDao().observeAll(); }

    public void markAttendance(long courseId, long dateMillis, int status) {
        io(() -> {
            AttendanceRecord r = new AttendanceRecord();
            r.courseId = courseId;
            r.date = dateMillis;
            r.status = status;
            db.attendanceDao().insert(r); // unique (course, date) -> replaces
            int attended = 0, total = 0;
            for (AttendanceRecord x : db.attendanceDao().forCourse(courseId)) {
                total++;
                if (x.status != AttendanceRecord.ABSENT) attended++;
            }
            if (total > 0 && attended * 100.0 / total < 75.0) {
                Course c = db.courseDao().get(courseId);
                NotificationHelper.show(app, NotificationHelper.CH_ATT, (int) (900000 + courseId),
                        "Attendance warning",
                        (c == null ? "A course" : c.label()) + " is at "
                                + Math.round(attended * 100.0 / total) + "% (below 75%).");
            }
        });
    }

    public void deleteAttendance(AttendanceRecord r) { io(() -> db.attendanceDao().delete(r)); }

    public void attendanceForCourse(long courseId, Callback<List<AttendanceRecord>> cb) {
        io(() -> post(cb, db.attendanceDao().forCourse(courseId)));
    }

    // ---------------- GPA ----------------
    public LiveData<List<Semester>> semesters() { return db.semesterDao().observeAll(); }
    /** Re-maps stored grade points by letter after the grading scale changes. */
    public void regradeAll(GradeScale scale) {
        io(() -> db.runInTransaction(() -> {
            for (GradeEntry g : db.gradeDao().all()) {
                int i = scale.indexOf(g.gradeLetter);
                if (i >= 0) {
                    g.gradePoint = scale.points[i];
                    db.gradeDao().update(g);
                }
            }
        }));
    }

    public LiveData<List<GradeEntry>> grades() { return db.gradeDao().observeAll(); }

    public void addSemester(String name, Callback<Long> cb) {
        io(() -> {
            Semester s = new Semester();
            s.name = name;
            post(cb, db.semesterDao().insert(s));
        });
    }

    public void updateSemester(Semester s) { io(() -> db.semesterDao().update(s)); }

    public void deleteSemester(Semester s) {
        io(() -> db.runInTransaction(() -> {
            db.gradeDao().deleteBySemester(s.id);
            db.semesterDao().delete(s);
        }));
    }

    public void saveGrade(GradeEntry g) {
        io(() -> { if (g.id == 0) db.gradeDao().insert(g); else db.gradeDao().update(g); });
    }

    public void deleteGrade(GradeEntry g) { io(() -> db.gradeDao().delete(g)); }


    // ---------------- assignments ----------------
    public LiveData<List<Assignment>> assignments() { return db.assignmentDao().observeAll(); }

    public void saveAssignment(Assignment a) {
        io(() -> {
            if (a.id == 0) a.id = db.assignmentDao().insert(a); else db.assignmentDao().update(a);
            ReminderScheduler.scheduleAssignment(app, a);
        });
    }

    public void deleteAssignment(Assignment a) {
        io(() -> { ReminderScheduler.cancelAssignment(app, a.id); db.assignmentDao().delete(a); });
    }

    // ---------------- exams ----------------
    public LiveData<List<Exam>> exams() { return db.examDao().observeAll(); }

    public void saveExam(Exam e) { io(() -> { if (e.id == 0) db.examDao().insert(e); else db.examDao().update(e); }); }

    public void deleteExam(Exam e) { io(() -> db.examDao().delete(e)); }

    // ---------------- notes ----------------
    public LiveData<List<Note>> searchNotes(String q) { return db.noteDao().search(q == null ? "" : q); }

    public void getNote(long id, Callback<Note> cb) { io(() -> post(cb, db.noteDao().get(id))); }

    public void saveNote(Note n, Callback<Long> cb) {
        io(() -> {
            n.updatedAt = System.currentTimeMillis();
            if (n.id == 0) n.id = db.noteDao().insert(n); else db.noteDao().update(n);
            post(cb, n.id);
        });
    }

    public void deleteNote(Note n) { io(() -> db.noteDao().delete(n)); }

    // ---------------- study timer ----------------
    public LiveData<List<StudySession>> studySessions() { return db.studyDao().observeAll(); }

    public void addStudySession(long courseId, long start, int seconds) {
        io(() -> {
            StudySession s = new StudySession();
            s.courseId = courseId;
            s.startMillis = start;
            s.seconds = seconds;
            db.studyDao().insert(s);
        });
    }

    // ---------------- course files ----------------
    public LiveData<List<CourseFile>> filesForCourse(long courseId) { return db.courseFileDao().observeForCourse(courseId); }

    public void addCourseFile(CourseFile f) { io(() -> db.courseFileDao().insert(f)); }

    public void deleteCourseFile(CourseFile f) { io(() -> db.courseFileDao().delete(f)); }

    // ---------------- PDF reader: recent files + bookmarks ----------------
    public LiveData<List<RecentFile>> recentFiles() { return db.recentFileDao().observeRecent(); }

    public void findRecent(String uri, Callback<RecentFile> cb) { io(() -> post(cb, db.recentFileDao().find(uri))); }

    public void upsertRecent(String uri, String name, int page, int pageCount) {
        io(() -> {
            RecentFile r = db.recentFileDao().find(uri);
            if (r == null) { r = new RecentFile(); r.uri = uri; }
            r.name = name;
            r.lastPage = page;
            r.pageCount = pageCount;
            r.lastOpened = System.currentTimeMillis();
            db.recentFileDao().insert(r);
        });
    }

    public void deleteRecent(RecentFile r) { io(() -> db.recentFileDao().delete(r)); }

    public void bookmarks(String uri, Callback<List<Bookmark>> cb) { io(() -> post(cb, db.bookmarkDao().forFile(uri))); }

    public void addBookmark(String uri, int page) {
        io(() -> {
            for (Bookmark b : db.bookmarkDao().forFile(uri)) if (b.page == page) return;
            Bookmark b = new Bookmark();
            b.fileUri = uri;
            b.page = page;
            b.createdAt = System.currentTimeMillis();
            db.bookmarkDao().insert(b);
        });
    }

    public void deleteBookmark(Bookmark b) { io(() -> db.bookmarkDao().delete(b)); }

    // ---------------- backup ----------------
    public Map<String, List<Map<String, Object>>> snapshotBlocking() { return BackupManager.snapshot(db); }

    public void restoreBlocking(Map<String, List<Map<String, Object>>> data) {
        BackupManager.restore(db, data);
        rescheduleAllBlocking();
        RoutineWidgetProvider.refresh(app);
    }

    public void rescheduleAll() { io(this::rescheduleAllBlocking); }

    private void rescheduleAllBlocking() {
        for (ClassSession s : db.sessionDao().all()) ReminderScheduler.scheduleClass(app, s);
        for (Assignment a : db.assignmentDao().all()) ReminderScheduler.scheduleAssignment(app, a);
    }
}
