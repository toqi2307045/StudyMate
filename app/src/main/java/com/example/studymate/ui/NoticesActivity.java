package com.example.studymate.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.databinding.ItemRowBinding;
import com.example.studymate.firebase.FirebaseSync;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoticesActivity extends BaseActivity {

    public static class ClassItem {
        public String id;
        public String name;
        public String code;
        public String role; // "admin" or "student"
        public String status; // "approved" or "pending"
    }

    private ActivityListBinding b;
    private final List<ClassItem> classes = new ArrayList<>();
    private ClassAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Class Notices", true);

        FirebaseUser user = FirebaseSync.currentUser();
        if (user == null) {
            UiUtil.toast(this, "Please sign in to access Class Notices");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        adapter = new ClassAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No classes joined yet.\nTap + to create or join a class.");

        b.fab.setOnClickListener(v -> showCreateOrJoinDialog());

        loadClasses();
    }

    private void loadClasses() {
        FirebaseUser user = FirebaseSync.currentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collectionGroup("members")
                .whereEqualTo("uid", user.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    List<String> classIds = new ArrayList<>();
                    Map<String, ClassItem> memberInfo = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String path = doc.getReference().getPath();
                        String[] segments = path.split("/");
                        if (segments.length >= 2) {
                            String classId = segments[1];
                            classIds.add(classId);
                            ClassItem item = new ClassItem();
                            item.id = classId;
                            item.role = doc.getString("role");
                            item.status = doc.getString("status");
                            memberInfo.put(classId, item);
                        }
                    }

                    if (classIds.isEmpty()) {
                        classes.clear();
                        adapter.notifyDataSetChanged();
                        b.empty.setVisibility(View.VISIBLE);
                        return;
                    }

                    classes.clear();
                    for (String cid : classIds) {
                        FirebaseFirestore.getInstance().collection("classes").document(cid)
                                .get()
                                .addOnSuccessListener(classDoc -> {
                                    if (classDoc.exists()) {
                                        ClassItem item = memberInfo.get(cid);
                                        if (item != null) {
                                            item.name = classDoc.getString("name");
                                            item.code = classDoc.getString("code");
                                            classes.add(item);
                                            adapter.notifyDataSetChanged();
                                            b.empty.setVisibility(classes.isEmpty() ? View.VISIBLE : View.GONE);
                                        }
                                    }
                                });
                    }
                });
    }

    private void showCreateOrJoinDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Class Notices")
                .setItems(new String[]{"Create Class", "Join Class"}, (d, which) -> {
                    if (which == 0) showCreateDialog();
                    else showJoinDialog();
                })
                .show();
    }

    private void showCreateDialog() {
        FormDialog f = new FormDialog(this);
        EditText nameInput = f.text("Class Name (e.g. CSE 301)", "");
        f.show("Create Class", "Create", () -> {
            String name = FormDialog.str(nameInput);
            if (name.isEmpty()) {
                nameInput.setError("Required");
                return false;
            }
            createClassInFirestore(name);
            return true;
        });
    }

    private void createClassInFirestore(String className) {
        FirebaseUser user = FirebaseSync.currentUser();
        if (user == null) return;

        String classId = UUID.randomUUID().toString();
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> classData = new HashMap<>();
        classData.put("id", classId);
        classData.put("name", className);
        classData.put("code", code);
        classData.put("adminUid", user.getUid());
        classData.put("createdAt", System.currentTimeMillis());

        db.collection("classes").document(classId)
                .set(classData)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("uid", user.getUid());
                    memberData.put("name", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                    memberData.put("role", "admin");
                    memberData.put("status", "approved");
                    memberData.put("joinedAt", System.currentTimeMillis());

                    db.collection("classes").document(classId)
                            .collection("members").document(user.getUid())
                            .set(memberData)
                            .addOnSuccessListener(v -> UiUtil.toast(this, "Class created! Code: " + code));
                })
                .addOnFailureListener(e -> UiUtil.toast(this, "Failed to create class: " + e.getMessage()));
    }

    private void showJoinDialog() {
        FormDialog f = new FormDialog(this);
        EditText codeInput = f.text("8-Character Class Code", "");
        f.show("Join Class", "Send Request", () -> {
            String code = FormDialog.str(codeInput).toUpperCase();
            if (code.length() < 6) {
                codeInput.setError("Enter valid class code");
                return false;
            }
            joinClassInFirestore(code);
            return true;
        });
    }

    private void joinClassInFirestore(String code) {
        FirebaseUser user = FirebaseSync.currentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("classes")
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        UiUtil.toast(this, "Invalid class code");
                        return;
                    }
                    DocumentSnapshot doc = qs.getDocuments().get(0);
                    String classId = doc.getId();

                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("uid", user.getUid());
                    memberData.put("name", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                    memberData.put("role", "student");
                    memberData.put("status", "pending");
                    memberData.put("joinedAt", System.currentTimeMillis());

                    FirebaseFirestore.getInstance().collection("classes").document(classId)
                            .collection("members").document(user.getUid())
                            .set(memberData)
                            .addOnSuccessListener(v -> UiUtil.toast(this, "Join request sent! Waiting for admin approval."))
                            .addOnFailureListener(e -> UiUtil.toast(this, "Failed to join: " + e.getMessage()));
                });
    }

    private class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.VH> {
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
            ClassItem c = classes.get(pos);
            h.b.strip.setBackgroundColor(0xFF3F51B5);
            h.b.title.setText(c.name);
            h.b.subtitle.setText("Role: " + c.role + " · Status: " + c.status);
            h.b.trailing.setText(c.status.equals("approved") ? "Open" : "Pending");

            h.b.card.setOnClickListener(v -> {
                if (!c.status.equals("approved")) {
                    UiUtil.toast(NoticesActivity.this, "Your request is pending admin approval.");
                    return;
                }
                Intent i = new Intent(NoticesActivity.this, ClassRoomActivity.class);
                i.putExtra("classId", c.id);
                i.putExtra("className", c.name);
                i.putExtra("role", c.role);
                i.putExtra("classCode", c.code);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() { return classes.size(); }
    }
}
