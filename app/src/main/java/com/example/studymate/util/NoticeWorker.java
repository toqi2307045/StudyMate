package com.example.studymate.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

public class NoticeWorker extends Worker {

    public NoticeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                NoticeWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                "notice_bg_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    public static void triggerImmediate(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NoticeWorker.class).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                "notice_immediate_check",
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();

        try {
            QuerySnapshot joinedClasses = Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .collection("joined_classes")
                            .get()
            );

            if (joinedClasses != null) {
                for (DocumentSnapshot classDoc : joinedClasses.getDocuments()) {
                    String status = classDoc.getString("status");
                    String classId = classDoc.getString("classId");
                    String className = classDoc.getString("name");

                    if ("approved".equals(status) && classId != null) {
                        QuerySnapshot newNotices = Tasks.await(
                                FirebaseFirestore.getInstance()
                                        .collection("classes")
                                        .document(classId)
                                        .collection("notices")
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .limit(10)
                                        .get()
                        );

                        if (newNotices != null) {
                            for (DocumentSnapshot noticeDoc : newNotices.getDocuments()) {
                                String noticeId = noticeDoc.getId();
                                String author = noticeDoc.getString("authorName");
                                String title = noticeDoc.getString("title");
                                String content = noticeDoc.getString("content");

                                if (!Prefs.isNoticeNotified(getApplicationContext(), noticeId)) {
                                    Prefs.markNoticeNotified(getApplicationContext(), noticeId);

                                    if (title != null) {
                                        String notifTitle = "📢 " + (className != null ? className : "Class Notice");
                                        String notifText = (author != null ? author : "Class Member") + ": " + title + (content == null || content.isEmpty() ? "" : "\n" + content);
                                        NotificationHelper.show(
                                                getApplicationContext(),
                                                NotificationHelper.CH_CLASS,
                                                noticeId.hashCode(),
                                                notifTitle,
                                                notifText
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return Result.retry();
        }

        return Result.success();
    }
}
