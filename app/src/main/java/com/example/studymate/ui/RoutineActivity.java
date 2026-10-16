package com.example.studymate.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.util.QrUtil;
import com.example.studymate.util.RoutineShare;
import com.example.studymate.util.TimeUtil;
import com.example.studymate.viewmodel.RoutineViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutineActivity extends BaseActivity {
    private static final int M_QR = 1, M_SHARE = 2, M_IMPORT = 3, M_COURSES = 4;

    private ActivityListBinding b;
    private RoutineViewModel vm;
    private final Map<Long, Course> courseMap = new HashMap<>();
    private List<Course> courses = new ArrayList<>();
    private RowAdapter<ClassSession> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Class Routine", true);
        vm = new ViewModelProvider(this).get(RoutineViewModel.class);

        TabLayout tabs = new TabLayout(this);
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        for (int d = 1; d <= 7; d++) tabs.addTab(tabs.newTab().setText(TimeUtil.dayShort(d)));
        int today = vm.day.getValue() == null ? TimeUtil.todayIso() : vm.day.getValue();
        TabLayout.Tab sel = tabs.getTabAt(today - 1);
        if (sel != null) sel.select();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { vm.day.setValue(tab.getPosition() + 1); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
        b.headerContainer.addView(tabs);

        adapter = new RowAdapter<>((r, s) -> {
            Course c = courseMap.get(s.courseId);
            r.strip.setBackgroundColor(c == null ? 0xFF9E9E9E : c.color);
            r.title.setText(c == null ? "(deleted course)" : c.label());
            r.subtitle.setText(TimeUtil.hm(s.startMin) + " – " + TimeUtil.hm(s.endMin)
                    + (s.room == null || s.room.isEmpty() ? "" : " · Room " + s.room));
            r.trailing.setText(c == null || c.teacher == null ? "" : c.teacher);
        });
        adapter.onClick(this::showForm);
        adapter.onLongClick(s -> UiUtil.confirm(this, "Delete class?", "Remove this class slot and its reminder?",
                "Delete", () -> vm.delete(s)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No classes on this day.\nTap + to add one.");

        vm.courses.observe(this, list -> {
            courses = list;
            courseMap.clear();
            for (Course c : list) courseMap.put(c.id, c);
            adapter.refresh();
        });
        vm.sessions.observe(this, list -> {
            adapter.submit(list);
            b.empty.setVisibility(list.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        });
        b.fab.setOnClickListener(v -> showForm(null));
    }

    private void showForm(ClassSession existing) {
        if (courses.isEmpty()) {
            UiUtil.toast(this, "Add a course first");
            startActivity(new Intent(this, CoursesActivity.class));
            return;
        }
        FormDialog f = new FormDialog(this);
        FormDialog.CourseField course = f.courseSpinner("Course", courses, existing == null ? 0 : existing.courseId, false);
        List<String> days = new ArrayList<>();
        for (int d = 1; d <= 7; d++) days.add(TimeUtil.dayName(d));
        int curDay = existing == null ? (vm.day.getValue() == null ? 1 : vm.day.getValue()) : existing.dayOfWeek;
        android.widget.Spinner day = f.spinner("Day", days, curDay - 1);
        FormDialog.TimeField start = f.time("Start time", existing == null ? 9 * 60 : existing.startMin);
        FormDialog.TimeField end = f.time("End time", existing == null ? 10 * 60 : existing.endMin);
        EditText room = f.text("Room", existing == null ? "" : existing.room);
        f.show(existing == null ? "New class" : "Edit class", "Save", () -> {
            if (course.selectedId() == 0) { UiUtil.toast(this, "Choose a course"); return false; }
            if (end.minutes <= start.minutes) { UiUtil.toast(this, "End time must be after start time"); return false; }
            ClassSession s = existing == null ? new ClassSession() : existing;
            s.courseId = course.selectedId();
            s.dayOfWeek = day.getSelectedItemPosition() + 1;
            s.startMin = start.minutes;
            s.endMin = end.minutes;
            s.room = FormDialog.str(room);
            vm.save(s);
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_QR, "Share as QR code");
        addMenu(menu, M_SHARE, "Share as text");
        addMenu(menu, M_IMPORT, "Import shared routine");
        addMenu(menu, M_COURSES, "Manage courses");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case M_QR:
                vm.exportText(text -> {
                    try {
                        Bitmap bmp = QrUtil.encode(text, 700);
                        ImageView iv = new ImageView(this);
                        iv.setImageBitmap(bmp);
                        iv.setPadding(24, 24, 24, 24);
                        new MaterialAlertDialogBuilder(this).setTitle("Scan to share your routine")
                                .setView(iv).setPositiveButton("Close", null).show();
                    } catch (Exception e) {
                        UiUtil.toast(this, "Routine is too large for a QR code - use Share as text");
                    }
                });
                return true;
            case M_SHARE:
                vm.exportText(text -> {
                    Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, "My class routine").putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, "Share routine"));
                });
                return true;
            case M_IMPORT:
                showImport();
                return true;
            case M_COURSES:
                startActivity(new Intent(this, CoursesActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showImport() {
        String prefill = "";
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item it = cm.getPrimaryClip().getItemAt(0);
            CharSequence t = it.getText();
            if (t != null && t.toString().startsWith(RoutineShare.HEADER)) prefill = t.toString();
        }
        FormDialog f = new FormDialog(this);
        EditText text = f.multiline("Paste the routine text your friend shared", prefill);
        f.show("Import routine", "Import", () -> {
            vm.importText(FormDialog.str(text), n -> UiUtil.toast(this,
                    n < 0 ? "That text is not a StudyMate routine" : "Imported " + n + " classes"));
            return true;
        });
    }
}
