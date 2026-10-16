package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.AttendanceRecord;
import com.example.studymate.data.entity.Course;
import com.example.studymate.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

public class AttendanceViewModel extends AndroidViewModel {

    public static class CourseAttendance {
        public Course course;
        public int present, absent, late;

        public int total() { return present + absent + late; }

        public int attended() { return present + late; }

        public double percent() { return total() == 0 ? 100.0 : attended() * 100.0 / total(); }

        public boolean belowThreshold() { return total() > 0 && percent() < 75.0; }

        /** Consecutive classes that must be attended to climb back to 75%. */
        public int classesNeeded() { return Math.max(0, 3 * total() - 4 * attended()); }
    }

    private final Repository repo;
    private final LiveData<List<Course>> courses;
    private final LiveData<List<AttendanceRecord>> records;
    public final MediatorLiveData<List<CourseAttendance>> items = new MediatorLiveData<>();

    public AttendanceViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        courses = repo.courses();
        records = repo.attendance();
        items.addSource(courses, c -> recompute());
        items.addSource(records, r -> recompute());
    }

    private void recompute() {
        List<Course> cs = courses.getValue();
        List<AttendanceRecord> rs = records.getValue();
        if (cs == null || rs == null) return;
        List<CourseAttendance> out = new ArrayList<>();
        for (Course c : cs) {
            CourseAttendance a = new CourseAttendance();
            a.course = c;
            for (AttendanceRecord r : rs) {
                if (r.courseId != c.id) continue;
                if (r.status == AttendanceRecord.PRESENT) a.present++;
                else if (r.status == AttendanceRecord.ABSENT) a.absent++;
                else a.late++;
            }
            out.add(a);
        }
        items.setValue(out);
    }

    public void markToday(long courseId, int status) {
        repo.markAttendance(courseId, TimeUtil.startOfDay(System.currentTimeMillis()), status);
    }

    public void mark(long courseId, long dateMillis, int status) {
        repo.markAttendance(courseId, TimeUtil.startOfDay(dateMillis), status);
    }

    public void delete(AttendanceRecord r) { repo.deleteAttendance(r); }

    public void history(long courseId, Repository.Callback<List<AttendanceRecord>> cb) {
        repo.attendanceForCourse(courseId, cb);
    }
}
