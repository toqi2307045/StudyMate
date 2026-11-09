package com.example.studymate.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.databinding.ItemRowBinding;
import com.example.studymate.firebase.FirebaseSync;
import com.example.studymate.util.Prefs;
import com.example.studymate.util.TimeUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassRoomActivity extends BaseActivity {
    private static final int M_MEMBERS = 1, M_CODE = 2;

    public static class NoticeItem {
        public String id;
        public String authorName;
        public String title;
        public String content;
        public long timestamp;
    }

    private ActivityListBinding b;
    private String classId, className, role, classCode;
    private final List<NoticeItem> notices = new ArrayList<>();
    private NoticeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        classId = getIntent().getStringExtra("classId");
        className = getIntent().getStringExtra("className");
        role = getIntent().getStringExtra("role");
        classCode = getIntent().getStringExtra("classCode");

        setupToolbar(b.toolbar, className != null ? className : "Classroom", true);

        adapter = new NoticeAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No notices published yet.");

        b.fab.setOnClickListener(v -> showPublishNoticeDialog());

        loadNotices();
    }

    private void loadNotices() {
        if (classId == null) return;
        FirebaseFirestore.getInstance().collection("classes").document(classId)
                .collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    notices.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        NoticeItem n = new NoticeItem();
                        n.id = doc.getId();
                        n.authorName = doc.getString("authorName");
                        n.title = doc.getString("title");
                        n.content = doc.getString("content");
                        Long ts = doc.getLong("timestamp");
                        n.timestamp = ts != null ? ts : System.currentTimeMillis();
                        notices.add(n);
                    }
                    adapter.notifyDataSetChanged();
                    b.empty.setVisibility(notices.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void showPublishNoticeDialog() {
        FormDialog f = new FormDialog(this);
        EditText title = f.text("Notice Title", "");
        EditText content = f.multiline("Notice Content", "");
        f.show("Publish Notice", "Publish", () -> {
            String t = FormDialog.str(title);
            String c = FormDialog.str(content);
            if (t.isEmpty() || c.isEmpty()) {
                UiUtil.toast(this, "Title and content required");
                return false;
            }
            publishNoticeInFirestore(t, c);
            return true;
        });
    }

    private void publishNoticeInFirestore(String title, String content) {
        FirebaseUser user = FirebaseSync.currentUser();
        if (user == null || classId == null) return;

        String noticeId = UUID.randomUUID().toString();
        Map<String, Object> noticeData = new HashMap<>();
        noticeData.put("id", noticeId);
        noticeData.put("authorName", Prefs.nickname(this));
        noticeData.put("title", title);
        noticeData.put("content", content);
        noticeData.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("classes").document(classId)
                .collection("notices").document(noticeId)
                .set(noticeData)
                .addOnSuccessListener(v -> UiUtil.toast(this, "Notice published"))
                .addOnFailureListener(e -> UiUtil.toast(this, "Failed to publish: " + e.getMessage()));
    }

    private void showMembersDialog() {
        if (!"admin".equals(role)) {
            UiUtil.toast(this, "Only admins can manage members.");
            return;
        }

        FirebaseFirestore.getInstance().collection("classes").document(classId)
                .collection("members")
                .get()
                .addOnSuccessListener(qs -> {
                    List<CharSequence> names = new ArrayList<>();
                    List<DocumentSnapshot> docs = qs.getDocuments();
                    for (DocumentSnapshot doc : docs) {
                        String name = doc.getString("name");
                        String status = doc.getString("status");
                        String mRole = doc.getString("role");
                        names.add(name + " (" + mRole + " - " + status + ")");
                    }

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Manage Members & Requests")
                            .setItems(names.toArray(new CharSequence[0]), (d, i) -> {
                                DocumentSnapshot selectedDoc = docs.get(i);
                                String uid = selectedDoc.getString("uid");
                                String status = selectedDoc.getString("status");
                                if ("pending".equals(status)) {
                                    showApprovalDialog(uid, selectedDoc.getString("name"));
                                }
                            })
                            .setNegativeButton("Close", null)
                            .show();
                });
    }

    private void showApprovalDialog(String uid, String name) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Approval Request: " + name)
                .setMessage("Approve or reject this student's request to join the class?")
                .setPositiveButton("Approve", (d, w) -> updateMemberStatus(uid, "approved"))
                .setNegativeButton("Reject", (d, w) -> updateMemberStatus(uid, "rejected"))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void updateMemberStatus(String uid, String status) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if ("rejected".equals(status)) {
            db.collection("classes").document(classId)
                    .collection("members").document(uid)
                    .delete();
            db.collection("users").document(uid)
                    .collection("joined_classes").document(classId)
                    .delete()
                    .addOnSuccessListener(v -> UiUtil.toast(this, "Request rejected"));
        } else {
            db.collection("classes").document(classId)
                    .collection("members").document(uid)
                    .update("status", status);
            db.collection("users").document(uid)
                    .collection("joined_classes").document(classId)
                    .update("status", status)
                    .addOnSuccessListener(v -> UiUtil.toast(this, "Student approved!"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ("admin".equals(role)) {
            addMenu(menu, M_MEMBERS, "Manage Requests");
            addMenu(menu, M_CODE, "Share Class Code");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_MEMBERS) {
            showMembersDialog();
            return true;
        } else if (item.getItemId() == M_CODE) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Class Code", classCode);
            clipboard.setPrimaryClip(clip);
            UiUtil.toast(this, "Class code copied: " + classCode);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final ItemRowBinding b;
            VH(ItemRowBinding b) { super(b.getRoot()); this.b = b; }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            NoticeItem n = notices.get(pos);
            h.b.strip.setBackgroundColor(0xFF3F51B5);
            h.b.title.setText(n.title);
            h.b.subtitle.setText("By " + n.authorName + "  ·  " + TimeUtil.formatDateTime(n.timestamp) + "\n\n" + n.content);
            h.b.trailing.setText("");
        }

        @Override
        public int getItemCount() { return notices.size(); }
    }
}
