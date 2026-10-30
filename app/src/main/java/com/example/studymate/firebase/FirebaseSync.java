package com.example.studymate.firebase;

import android.content.Context;

import com.example.studymate.data.Repository;
import com.example.studymate.util.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSync {

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }

    public static FirebaseUser currentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static boolean isSignedIn() {
        return currentUser() != null;
    }

    public static void pushToCloud(Context context, SyncCallback callback) {
        FirebaseUser user = currentUser();
        if (user == null) {
            if (callback != null) callback.onError("You must be signed in to sync data to the cloud.");
            return;
        }

        Repository repo = Repository.get(context);
        AppExecutors.io(() -> {
            try {
                Map<String, List<Map<String, Object>>> snapshot = repo.snapshotBlocking();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String uid = user.getUid();

                int[] pending = {snapshot.size()};
                boolean[] failed = {false};

                if (snapshot.isEmpty()) {
                    AppExecutors.main(() -> {
                        if (callback != null) callback.onSuccess();
                    });
                    return;
                }

                for (Map.Entry<String, List<Map<String, Object>>> entry : snapshot.entrySet()) {
                    String table = entry.getKey();
                    Map<String, Object> tableData = new HashMap<>();
                    tableData.put("rows", entry.getValue());
                    tableData.put("updatedAt", System.currentTimeMillis());

                    db.collection("users")
                            .document(uid)
                            .collection("data")
                            .document(table)
                            .set(tableData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                synchronized (pending) {
                                    pending[0]--;
                                    if (pending[0] <= 0 && !failed[0]) {
                                        AppExecutors.main(() -> {
                                            if (callback != null) callback.onSuccess();
                                        });
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                synchronized (pending) {
                                    if (!failed[0]) {
                                        failed[0] = true;
                                        AppExecutors.main(() -> {
                                            if (callback != null) callback.onError("Sync failed: " + e.getMessage());
                                        });
                                    }
                                }
                            });
                }
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    if (callback != null) callback.onError("Failed to read local data: " + e.getMessage());
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static void pullFromCloud(Context context, SyncCallback callback) {
        FirebaseUser user = currentUser();
        if (user == null) {
            if (callback != null) callback.onError("You must be signed in to pull cloud data.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .collection("data")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    AppExecutors.io(() -> {
                        try {
                            Map<String, List<Map<String, Object>>> data = new HashMap<>();
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String table = doc.getId();
                                List<Map<String, Object>> rows = (List<Map<String, Object>>) doc.get("rows");
                                if (rows != null) {
                                    data.put(table, rows);
                                }
                            }
                            if (!data.isEmpty()) {
                                Repository.get(context).restoreBlocking(data);
                            }
                            AppExecutors.main(() -> {
                                if (callback != null) callback.onSuccess();
                            });
                        } catch (Exception e) {
                            AppExecutors.main(() -> {
                                if (callback != null) callback.onError("Restore failed: " + e.getMessage());
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Failed to fetch cloud data: " + e.getMessage());
                });
    }
}
