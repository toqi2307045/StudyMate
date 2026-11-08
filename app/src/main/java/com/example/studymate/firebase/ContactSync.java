package com.example.studymate.firebase;

import android.content.Context;

import com.example.studymate.data.AppDatabase;
import com.example.studymate.data.dao.ContactDao;
import com.example.studymate.data.entity.Contact;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.ContactPhotos;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Two-way contact sync: users/{uid}/contacts/{syncId}, one document per contact.
 * Newest updatedAt wins. A deleted contact is kept as a tiny tombstone {deleted:true} so other
 * devices remove it too. Firestore queues writes while offline, so saves/deletes made without a
 * connection are sent automatically later.
 */
public final class ContactSync {
    private ContactSync() {}

    private static CollectionReference col() {
        com.google.firebase.auth.FirebaseUser u = FirebaseSync.currentUser();
        if (u == null) return null;
        return FirebaseFirestore.getInstance().collection("users").document(u.getUid()).collection("contacts");
    }

    private static long num(Long v) { return v == null ? 0 : v; }

    private static Map<String, Object> toMap(Contact c) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", c.name);
        m.put("type", c.type);
        m.put("phone", c.phone);
        m.put("email", c.email);
        m.put("rollOrDesignation", c.rollOrDesignation);
        m.put("updatedAt", c.updatedAt);
        m.put("deleted", false);
        String thumb = ContactPhotos.thumbBase64(c.photoPath);
        if (thumb != null) m.put("photo", thumb);
        return m;
    }

    private static void apply(Context app, Contact c, DocumentSnapshot d) {
        c.name = d.getString("name");
        Long t = d.getLong("type");
        c.type = t == null ? Contact.TEACHER : t.intValue();
        c.phone = d.getString("phone");
        c.email = d.getString("email");
        c.rollOrDesignation = d.getString("rollOrDesignation");
        c.updatedAt = num(d.getLong("updatedAt"));
        String photo = d.getString("photo");
        if (photo != null && !photo.isEmpty()) {
            String p = ContactPhotos.saveFromBase64(app, c.syncId, photo);
            if (p != null) c.photoPath = p;
        } else {
            ContactPhotos.delete(c.photoPath);
            c.photoPath = null;
        }
    }

    /** Fire-and-forget upload of one contact. Call off the main thread (it reads the photo file). */
    public static void pushOne(Contact c) {
        CollectionReference col = col();
        if (col == null || c.syncId == null) return;
        col.document(c.syncId).set(toMap(c));
    }

    /** Replaces the remote document with a tombstone so other devices delete the contact as well. */
    public static void deleteOne(String syncId) {
        CollectionReference col = col();
        if (col == null || syncId == null) return;
        Map<String, Object> t = new HashMap<>();
        t.put("deleted", true);
        t.put("updatedAt", System.currentTimeMillis());
        col.document(syncId).set(t);
    }

    /** Merges cloud and local contacts in both directions. Callback runs on the main thread. */
    public static void syncNow(Context ctx, FirebaseSync.SyncCallback cb) {
        CollectionReference col = col();
        if (col == null) {
            if (cb != null) cb.onError("Sign in to sync contacts.");
            return;
        }
        Context app = ctx.getApplicationContext();
        col.get().addOnSuccessListener(qs -> AppExecutors.io(() -> {
            try {
                ContactDao dao = AppDatabase.getInstance(app).contactDao();
                Map<String, Contact> local = new HashMap<>();
                for (Contact c : dao.all()) local.put(c.syncId, c);
                Set<String> seen = new HashSet<>();

                for (DocumentSnapshot d : qs.getDocuments()) {
                    String id = d.getId();
                    seen.add(id);
                    long remoteAt = num(d.getLong("updatedAt"));
                    boolean tomb = Boolean.TRUE.equals(d.getBoolean("deleted"));
                    Contact l = local.get(id);
                    if (tomb) {
                        if (l == null) continue;
                        if (l.updatedAt <= remoteAt) {
                            ContactPhotos.delete(l.photoPath);
                            dao.delete(l);
                        } else {
                            pushOne(l); // edited after it was deleted elsewhere: keep it
                        }
                    } else if (l == null) {
                        Contact n = new Contact();
                        n.syncId = id;
                        apply(app, n, d);
                        dao.insert(n);
                    } else if (remoteAt > l.updatedAt) {
                        apply(app, l, d);
                        dao.update(l);
                    } else if (l.updatedAt > remoteAt) {
                        pushOne(l);
                    }
                }
                for (Contact l : local.values()) {
                    if (!seen.contains(l.syncId)) pushOne(l);
                }
                AppExecutors.main(() -> { if (cb != null) cb.onSuccess(); });
            } catch (Exception e) {
                AppExecutors.main(() -> { if (cb != null) cb.onError("Contact sync failed: " + e.getMessage()); });
            }
        })).addOnFailureListener(e -> {
            if (cb != null) cb.onError("Couldn't reach the cloud: " + e.getMessage());
        });
    }
}
