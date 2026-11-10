package com.example.studymate.firebase;

import android.content.Context;

import com.example.studymate.util.Prefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RealtimeDbManager {

    private static FirebaseDatabase getDb() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        try {
            db.setPersistenceEnabled(true);
        } catch (Exception ignored) { }
        return db;
    }

    public static void setupUserPresence() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference connectedRef = getDb().getReference(".info/connected");
        DatabaseReference presenceRef = getDb().getReference("users_presence").child(user.getUid());

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(connected)) {
                    Map<String, Object> onlineStatus = new HashMap<>();
                    onlineStatus.put("online", true);
                    onlineStatus.put("lastSeen", ServerValue.TIMESTAMP);
                    onlineStatus.put("email", user.getEmail());

                    Map<String, Object> offlineStatus = new HashMap<>();
                    offlineStatus.put("online", false);
                    offlineStatus.put("lastSeen", ServerValue.TIMESTAMP);

                    presenceRef.onDisconnect().setValue(offlineStatus);
                    presenceRef.setValue(onlineStatus);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    public interface LiveNoticeCallback {
        void onNoticeReceived(String id, String senderUid, String className, String author, String title, String content);
    }

    public static void publishClassNotice(Context context, String classId, String className, String title, String content) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || classId == null || title == null) return;

        DatabaseReference ref = getDb().getReference("live_notices").child(classId).push();
        String author = Prefs.nickname(context);

        Map<String, Object> data = new HashMap<>();
        data.put("id", ref.getKey());
        data.put("senderUid", user.getUid());
        data.put("author", author);
        data.put("className", className);
        data.put("title", title);
        data.put("content", content);
        data.put("timestamp", ServerValue.TIMESTAMP);

        ref.setValue(data);
    }

    public static void listenClassNotices(String classId, LiveNoticeCallback callback) {
        if (classId == null) return;
        DatabaseReference ref = getDb().getReference("live_notices").child(classId);

        ref.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String id = snapshot.child("id").getValue(String.class);
                String senderUid = snapshot.child("senderUid").getValue(String.class);
                String className = snapshot.child("className").getValue(String.class);
                String author = snapshot.child("author").getValue(String.class);
                String title = snapshot.child("title").getValue(String.class);
                String content = snapshot.child("content").getValue(String.class);

                if (title != null && callback != null) {
                    callback.onNoticeReceived(id, senderUid, className, author, title, content);
                }
            }

            @Override public void onChildChanged(DataSnapshot s, String p) {}
            @Override public void onChildRemoved(DataSnapshot s) {}
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override public void onCancelled(DatabaseError e) {}
        });
    }
}
