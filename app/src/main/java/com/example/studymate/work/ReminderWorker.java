package com.example.studymate.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.studymate.data.AppDatabase;
import com.example.studymate.data.entity.Assignment;
import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.util.NotificationHelper;
import com.example.studymate.util.ReminderScheduler;
import com.example.studymate.util.TimeUtil;

/** Background worker (WorkManager) that posts class / assignment / pomodoro notifications. */
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        String kind = getInputData().getString("kind");
        long id = getInputData().getLong("id", 0);
        AppDatabase db = AppDatabase.getInstance(c);
        if (kind == null) return Result.success();

        switch (kind) {
            case "class": {
                ClassSession s = db.sessionDao().get(id);
                if (s == null) return Result.success();
                Course course = db.courseDao().get(s.courseId);
                String name = course == null ? "Class" : course.label();
                String room = (s.room == null || s.room.isEmpty()) ? "" : " · Room " + s.room;
                NotificationHelper.show(c, NotificationHelper.CH_CLASS, (int) (1000 + id),
                        "Upcoming: " + name, "Starts at " + TimeUtil.hm(s.startMin) + room);
                ReminderScheduler.scheduleClass(c, s); // re-arm for next week
                break;
            }
            case "assignment": {
                Assignment a = db.assignmentDao().get(id);
                if (a != null && !a.done) {
                    NotificationHelper.show(c, NotificationHelper.CH_TASK, (int) (500000 + id),
                            "Due soon: " + a.title, "Due " + TimeUtil.formatDateTime(a.dueMillis));
                }
                break;
            }
            case "pomodoro": {
                NotificationHelper.show(c, NotificationHelper.CH_POMO, 42,
                        getInputData().getString("title"), getInputData().getString("text"));
                break;
            }
            default:
                break;
        }
        return Result.success();
    }
}
