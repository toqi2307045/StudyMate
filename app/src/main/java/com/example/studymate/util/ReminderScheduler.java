package com.example.studymate.util;
import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.studymate.data.entity.Assignment;
import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.work.ReminderWorker;

import java.util.concurrent.TimeUnit;

/** Schedules reminders with WorkManager one-time requests (classes re-arm themselves weekly). */
public final class ReminderScheduler {
    private ReminderScheduler() {}

    private static void enqueue(Context c, String tag, String kind, long id, long triggerAt, String title, String text) {
        long delay = Math.max(0, triggerAt - System.currentTimeMillis());
        Data data = new Data.Builder().putString("kind", kind).putLong("id", id)
                .putString("title", title).putString("text", text).build();
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build();
        WorkManager.getInstance(c).enqueue(req);
    }

    public static void scheduleClass(Context c, ClassSession s) {
        WorkManager.getInstance(c).cancelAllWorkByTag("class_" + s.id);
        long lead = Prefs.classLeadMin(c) * 60_000L;
        long classStart = TimeUtil.nextWeekly(s.dayOfWeek, s.startMin, System.currentTimeMillis() + lead + 30_000L);
        enqueue(c, "class_" + s.id, "class", s.id, classStart - lead, null, null);
    }

    public static void cancelClass(Context c, long id) {
        WorkManager.getInstance(c).cancelAllWorkByTag("class_" + id);
    }

    public static void scheduleAssignment(Context c, Assignment a) {
        WorkManager.getInstance(c).cancelAllWorkByTag("task_" + a.id);
        if (a.done || a.dueMillis == 0) return;
        long now = System.currentTimeMillis();
        long first = a.dueMillis - Prefs.assignLeadHours(c) * 3_600_000L;
        long fallback = a.dueMillis - 15 * 60_000L;
        long at = first > now ? first : (fallback > now ? fallback : 0);
        if (at == 0) return;
        enqueue(c, "task_" + a.id, "assignment", a.id, at, null, null);
    }

    public static void cancelAssignment(Context c, long id) {
        WorkManager.getInstance(c).cancelAllWorkByTag("task_" + id);
    }

    public static void schedulePomodoro(Context c, long delayMs, String title, String text) {
        cancelPomodoro(c);
        enqueue(c, "pomodoro", "pomodoro", 0, System.currentTimeMillis() + delayMs, title, text);
    }

    public static void cancelPomodoro(Context c) {
        WorkManager.getInstance(c).cancelAllWorkByTag("pomodoro");
    }
}
