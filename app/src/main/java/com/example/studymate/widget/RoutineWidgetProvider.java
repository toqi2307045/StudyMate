package com.example.studymate.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.studymate.R;
import com.example.studymate.data.AppDatabase;
import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.ui.MainActivity;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.TimeUtil;

import java.util.List;

/** Home-screen widget listing today's classes. */
public class RoutineWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        PendingResult pending = goAsync();
        AppExecutors.io(() -> {
            try {
                update(context, manager, ids);
            } finally {
                pending.finish();
            }
        });
    }

    private static void update(Context c, AppWidgetManager manager, int[] ids) {
        AppDatabase db = AppDatabase.getInstance(c);
        List<ClassSession> today = db.sessionDao().forDay(TimeUtil.todayIso());
        StringBuilder sb = new StringBuilder();
        for (ClassSession s : today) {
            Course course = db.courseDao().get(s.courseId);
            sb.append(TimeUtil.hm(s.startMin)).append("  ")
                    .append(course == null ? "Class" : course.label());
            if (s.room != null && !s.room.isEmpty()) sb.append("  (").append(s.room).append(")");
            sb.append('\n');
        }
        String body = today.isEmpty() ? "No classes today" : sb.toString().trim();
        PendingIntent pi = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_routine);
            rv.setTextViewText(R.id.widget_title, "Today · " + TimeUtil.dayName(TimeUtil.todayIso()));
            rv.setTextViewText(R.id.widget_body, body);
            rv.setOnClickPendingIntent(R.id.widget_root, pi);
            manager.updateAppWidget(id, rv);
        }
    }

    /** Call from any thread after routine data changes. */
    public static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, RoutineWidgetProvider.class));
        if (ids == null || ids.length == 0) return;
        AppExecutors.io(() -> update(c.getApplicationContext(), m, ids));
    }
}
