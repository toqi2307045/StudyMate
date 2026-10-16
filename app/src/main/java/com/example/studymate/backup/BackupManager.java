package com.example.studymate.backup;

import com.example.studymate.data.AppDatabase;
import com.example.studymate.data.entity.Assignment;
import com.example.studymate.data.entity.AttendanceRecord;
import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Exam;
import com.example.studymate.data.entity.GradeEntry;
import com.example.studymate.data.entity.Note;
import com.example.studymate.data.entity.Semester;
import com.example.studymate.data.entity.StudySession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Reads/writes every table as generic row maps so any DataFormat can export or import it. */
public final class BackupManager {
    private BackupManager() {}

    private static class Table<E> {
        final String name;
        final Class<E> cls;
        final Supplier<List<E>> reader;
        final Consumer<List<E>> writer;

        Table(String name, Class<E> cls, Supplier<List<E>> reader, Consumer<List<E>> writer) {
            this.name = name; this.cls = cls; this.reader = reader; this.writer = writer;
        }

        List<Map<String, Object>> export() {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (E e : reader.get()) rows.add(EntityCodec.toMap(e));
            return rows;
        }

        void importRows(List<Map<String, Object>> rows) {
            List<E> list = new ArrayList<>();
            for (Map<String, Object> m : rows) list.add(EntityCodec.fromMap(cls, m));
            writer.accept(list);
        }
    }

    private static List<Table<?>> tables(AppDatabase db) {
        List<Table<?>> t = new ArrayList<>();
        t.add(new Table<>("courses", Course.class, db.courseDao()::all, db.courseDao()::insertAll));
        t.add(new Table<>("class_sessions", ClassSession.class, db.sessionDao()::all, db.sessionDao()::insertAll));
        t.add(new Table<>("attendance", AttendanceRecord.class, db.attendanceDao()::all, db.attendanceDao()::insertAll));
        t.add(new Table<>("semesters", Semester.class, db.semesterDao()::all, db.semesterDao()::insertAll));
        t.add(new Table<>("grades", GradeEntry.class, db.gradeDao()::all, db.gradeDao()::insertAll));
        t.add(new Table<>("assignments", Assignment.class, db.assignmentDao()::all, db.assignmentDao()::insertAll));
        t.add(new Table<>("exams", Exam.class, db.examDao()::all, db.examDao()::insertAll));
        t.add(new Table<>("notes", Note.class, db.noteDao()::all, db.noteDao()::insertAll));
        t.add(new Table<>("study_sessions", StudySession.class, db.studyDao()::all, db.studyDao()::insertAll));
        return t;
    }

    /** Must run off the main thread. */
    public static Map<String, List<Map<String, Object>>> snapshot(AppDatabase db) {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        for (Table<?> t : tables(db)) out.put(t.name, t.export());
        return out;
    }

    /** Replaces all data with the given snapshot, atomically. Must run off the main thread. */
    public static void restore(AppDatabase db, Map<String, List<Map<String, Object>>> data) {
        db.runInTransaction(() -> {
            db.courseDao().clear();
            db.sessionDao().clear();
            db.attendanceDao().clear();
            db.semesterDao().clear();
            db.gradeDao().clear();
            db.assignmentDao().clear();
            db.examDao().clear();
            db.noteDao().clear();
            db.studyDao().clear();
            for (Table<?> t : tables(db)) {
                List<Map<String, Object>> rows = data.get(t.name);
                if (rows != null) t.importRows(rows);
            }
        });
    }
}
