package com.example.studymate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;

import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Note;
import com.example.studymate.databinding.ActivityNoteEditBinding;
import com.example.studymate.viewmodel.NoteViewModel;

import java.util.ArrayList;
import java.util.List;

public class NoteEditActivity extends BaseActivity {
    private static final int M_SAVE = 1, M_DELETE = 2;

    private ActivityNoteEditBinding b;
    private NoteViewModel vm;
    private Note note = new Note();
    private List<Course> courses = new ArrayList<>();
    private boolean loaded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityNoteEditBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Note", true);
        vm = new ViewModelProvider(this).get(NoteViewModel.class);

        long id = getIntent().getLongExtra("id", 0);
        if (id != 0) {
            loaded = false;
            vm.load(id, n -> {
                if (n == null) { finish(); return; }
                note = n;
                b.noteTitle.setText(n.title);
                b.noteBody.setText(n.body);
                b.noteTags.setText(n.tags);
                loaded = true;
                selectCourse();
            });
        }
        vm.courses.observe(this, list -> {
            courses = list;
            List<String> names = new ArrayList<>();
            names.add("Link to course (optional)");
            for (Course c : list) names.add(c.label());
            ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            b.noteCourse.setAdapter(ad);
            selectCourse();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveNote(true);
            }
        });
    }

    private void selectCourse() {
        if (!loaded) return;
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).id == note.courseId) { b.noteCourse.setSelection(i + 1); return; }
        }
    }

    private void saveNote(boolean closeAfter) {
        String title = b.noteTitle.getText().toString().trim();
        String body = b.noteBody.getText().toString();
        if (title.isEmpty() && body.trim().isEmpty()) {
            if (closeAfter) finish();
            return;
        }
        if (!loaded) return;
        note.title = title;
        note.body = body;
        note.tags = b.noteTags.getText().toString().trim();
        int p = b.noteCourse.getSelectedItemPosition();
        note.courseId = p <= 0 || p > courses.size() ? 0 : courses.get(p - 1).id;
        vm.save(note, savedId -> {
            if (closeAfter) finish();
            else UiUtil.toast(this, "Saved");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem s = menu.add(Menu.NONE, M_SAVE, Menu.NONE, "Save");
        s.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        addMenu(menu, M_DELETE, "Delete note");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_SAVE) {
            saveNote(false);
            return true;
        } else if (item.getItemId() == M_DELETE) {
            UiUtil.confirm(this, "Delete note?", "This cannot be undone.", "Delete", () -> {
                if (note.id != 0) vm.delete(note);
                finish();
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
