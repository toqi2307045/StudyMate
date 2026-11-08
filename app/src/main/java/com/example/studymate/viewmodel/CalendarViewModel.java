package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Assignment;
import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Exam;
import com.example.studymate.util.TimeUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Combines the weekly routine and assignment deadlines into one list for the selected date. */
public class CalendarViewModel extends AndroidViewModel {

    public static class Entry {
        public static final int CLASS = 0, ASSIGNMENT = 1, EXAM = 2;
        public int type;
        public int color;
        public String title, subtitle, trailing;
        public int minuteOfDay;
        /** Only set for ASSIGNMENT entries. */
        public Assignment assignment;
        public boolean done;
    }

    public static final int STATUS_NONE = 0;
    public static final int STATUS_CLASS_ONLY = 1;      // Blue (0xFF2196F3)
    public static final int STATUS_ASSIGNMENT_ONLY = 2; // Orange (0xFFFF9800)
    public static final int STATUS_EXAM_ONLY = 3;       // Red (0xFFE53935)
    public static final int STATUS_MULTIPLE = 4;        // Purple (0xFF9C27B0)

    public static int getStatusColor(int status) {
        switch (status) {
            case STATUS_CLASS_ONLY: return 0xFF2196F3;
            case STATUS_ASSIGNMENT_ONLY: return 0xFFFF9800;
            case STATUS_EXAM_ONLY: return 0xFFE53935;
            case STATUS_MULTIPLE: return 0xFF9C27B0;
            default: return 0xFF9E9E9E;
        }
    }

    private static final String[] PRIORITY_NAMES = {"Low", "Medium", "High"};

    private final Repository repo;
    private final LiveData<List<Course>> courses;
    private final LiveData<List<Assignment>> assignments;
    private final LiveData<List<Exam>> exams;
    private final LiveData<List<ClassSession>> sessions;
    public final LiveData<List<ClassSession>> allClassSessions;

    /** Start of the selected day, epoch millis (local time). */
    public final MutableLiveData<Long> date =
            new MutableLiveData<>(TimeUtil.startOfDay(System.currentTimeMillis()));
    public final MutableLiveData<ZonedDateTime> currentMonth =
            new MutableLiveData<>(ZonedDateTime.now());
    public final MediatorLiveData<List<Entry>> entries = new MediatorLiveData<>();
    public final MediatorLiveData<List<CalendarDay>> monthDays = new MediatorLiveData<>();

    public CalendarViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        courses = repo.courses();
        assignments = repo.assignments();
        exams = repo.exams();
        allClassSessions = repo.allClassSessions();
        sessions = Transformations.switchMap(date, d -> repo.sessionsForDay(isoDay(d)));

        // "date" is intentionally not a source: the switchMap above re-emits `sessions`
        // whenever the date changes, so recompute() never sees a stale weekday.
        entries.addSource(courses, c -> recompute());
        entries.addSource(assignments, a -> recompute());
        entries.addSource(exams, e -> recompute());
        entries.addSource(sessions, s -> recompute());

        monthDays.addSource(currentMonth, m -> recomputeMonthGrid());
        monthDays.addSource(date, d -> recomputeMonthGrid());
        monthDays.addSource(assignments, a -> recomputeMonthGrid());
        monthDays.addSource(exams, e -> recomputeMonthGrid());
        monthDays.addSource(allClassSessions, s -> recomputeMonthGrid());
    }

    public void prevMonth() {
        ZonedDateTime cur = currentMonth.getValue();
        if (cur != null) currentMonth.setValue(cur.minusMonths(1));
    }

    public void nextMonth() {
        ZonedDateTime cur = currentMonth.getValue();
        if (cur != null) currentMonth.setValue(cur.plusMonths(1));
    }

    public void recomputeMonthGrid() {
        ZonedDateTime month = currentMonth.getValue();
        Long selectedMillis = date.getValue();
        List<Assignment> as = assignments.getValue();
        List<Exam> exList = exams.getValue();
        List<ClassSession> allSs = allClassSessions.getValue();

        if (month == null || selectedMillis == null) return;

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime selectedDate = Instant.ofEpochMilli(selectedMillis).atZone(zone);
        ZonedDateTime today = ZonedDateTime.now(zone);

        ZonedDateTime firstOfMonth = month.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 = Mon ... 7 = Sun
        ZonedDateTime gridStart = firstOfMonth.minusDays(dayOfWeek - 1);

        boolean[] hasClassOnDay = new boolean[8];
        if (allSs != null) {
            for (ClassSession s : allSs) {
                if (s.dayOfWeek >= 1 && s.dayOfWeek <= 7) {
                    hasClassOnDay[s.dayOfWeek] = true;
                }
            }
        }

        Map<Long, Boolean> hasAssignmentOnDay = new HashMap<>();
        if (as != null) {
            for (Assignment a : as) {
                if (a.dueMillis > 0) {
                    hasAssignmentOnDay.put(TimeUtil.startOfDay(a.dueMillis), true);
                }
            }
        }

        Map<Long, Boolean> hasExamOnDay = new HashMap<>();
        if (exList != null) {
            for (Exam ex : exList) {
                if (ex.dateMillis > 0) {
                    hasExamOnDay.put(TimeUtil.startOfDay(ex.dateMillis), true);
                }
            }
        }

        List<CalendarDay> days = new ArrayList<>();
        ZonedDateTime curr = gridStart;

        for (int i = 0; i < 42; i++) {
            long dayStartMillis = TimeUtil.startOfDay(curr.toInstant().toEpochMilli());
            boolean isCurMonth = curr.getMonthValue() == month.getMonthValue() && curr.getYear() == month.getYear();
            boolean isSelected = curr.toLocalDate().equals(selectedDate.toLocalDate());
            boolean isToday = curr.toLocalDate().equals(today.toLocalDate());

            boolean hasClass = hasClassOnDay[curr.getDayOfWeek().getValue()];
            boolean hasAssignment = Boolean.TRUE.equals(hasAssignmentOnDay.get(dayStartMillis));
            boolean hasExam = Boolean.TRUE.equals(hasExamOnDay.get(dayStartMillis));

            int count = (hasClass ? 1 : 0) + (hasAssignment ? 1 : 0) + (hasExam ? 1 : 0);

            int status = STATUS_NONE;
            if (count >= 2) status = STATUS_MULTIPLE;
            else if (hasClass) status = STATUS_CLASS_ONLY;
            else if (hasAssignment) status = STATUS_ASSIGNMENT_ONLY;
            else if (hasExam) status = STATUS_EXAM_ONLY;

            days.add(new CalendarDay(curr.toLocalDate(), isCurMonth, isSelected, isToday, status));
            curr = curr.plusDays(1);
        }

        monthDays.setValue(days);
    }

    private static int isoDay(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).getDayOfWeek().getValue();
    }

    private void recompute() {
        List<Course> cs = courses.getValue();
        List<Assignment> as = assignments.getValue();
        List<Exam> exList = exams.getValue();
        List<ClassSession> ss = sessions.getValue();
        Long d = date.getValue();
        if (cs == null || as == null || ss == null || d == null) return;

        Map<Long, Course> byId = new HashMap<>();
        for (Course c : cs) byId.put(c.id, c);

        List<Entry> out = new ArrayList<>();

        for (ClassSession s : ss) {
            Course c = byId.get(s.courseId);
            Entry e = new Entry();
            e.type = Entry.CLASS;
            e.color = c == null ? 0xFF9E9E9E : c.color;
            e.title = c == null ? "(deleted course)" : c.label();
            e.subtitle = TimeUtil.hm(s.startMin) + " – " + TimeUtil.hm(s.endMin)
                    + (s.room == null || s.room.isEmpty() ? "" : " · Room " + s.room);
            e.trailing = "Class";
            e.minuteOfDay = s.startMin;
            out.add(e);
        }

        ZoneId zone = ZoneId.systemDefault();
        for (Assignment a : as) {
            if (a.dueMillis == 0 || TimeUtil.startOfDay(a.dueMillis) != d) continue;
            Course c = byId.get(a.courseId);
            ZonedDateTime due = Instant.ofEpochMilli(a.dueMillis).atZone(zone);
            Entry e = new Entry();
            e.type = Entry.ASSIGNMENT;
            e.color = c == null ? 0xFF9E9E9E : c.color;
            e.title = "📝 " + a.title;
            e.subtitle = (c == null ? "" : c.label() + "  ·  ")
                    + "Due " + TimeUtil.hm(due.getHour() * 60 + due.getMinute())
                    + "  ·  " + PRIORITY_NAMES[Math.max(0, Math.min(2, a.priority))];
            e.trailing = "Due";
            e.minuteOfDay = due.getHour() * 60 + due.getMinute();
            e.assignment = a;
            e.done = a.done;
            out.add(e);
        }

        if (exList != null) {
            for (Exam ex : exList) {
                if (ex.dateMillis == 0 || TimeUtil.startOfDay(ex.dateMillis) != d) continue;
                Course c = byId.get(ex.courseId);
                ZonedDateTime exTime = Instant.ofEpochMilli(ex.dateMillis).atZone(zone);
                Entry e = new Entry();
                e.type = Entry.EXAM;
                e.color = c == null ? 0xFFE53935 : c.color;
                e.title = "⏳ " + ex.title;
                e.subtitle = (c == null ? "" : c.label() + "  ·  ")
                        + "Exam at " + TimeUtil.hm(exTime.getHour() * 60 + exTime.getMinute())
                        + (ex.room == null || ex.room.isEmpty() ? "" : " · Room " + ex.room);
                e.trailing = "Exam";
                e.minuteOfDay = exTime.getHour() * 60 + exTime.getMinute();
                out.add(e);
            }
        }

        Collections.sort(out, (x, y) -> x.minuteOfDay != y.minuteOfDay
                ? Integer.compare(x.minuteOfDay, y.minuteOfDay)
                : Integer.compare(x.type, y.type));
        entries.setValue(out);
    }

    public void setDone(Assignment a, boolean done) {
        a.done = done;
        repo.saveAssignment(a);
    }
}
