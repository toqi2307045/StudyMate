package com.example.studymate.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Note;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.util.BiometricGate;
import com.example.studymate.util.Prefs;
import com.example.studymate.viewmodel.NoteViewModel;

import java.util.HashMap;
import java.util.Map;

public class NotesActivity extends BaseActivity {
    private ActivityListBinding b;
    private NoteViewModel vm;
    private final Map<Long, Course> courseMap = new HashMap<>();
    private RowAdapter<Note> adapter;
    private boolean unlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Notes", true);
        vm = new ViewModelProvider(this).get(NoteViewModel.class);
        unlocked = savedInstanceState != null && savedInstanceState.getBoolean("unlocked");

        adapter = new RowAdapter<>((r, n) -> {
            Course c = courseMap.get(n.courseId);
            r.strip.setBackgroundColor(c == null ? 0xFF9E9E9E : c.color);
            r.title.setText(n.title == null || n.title.isEmpty() ? "(untitled)" : n.title);
            String body = n.body == null ? "" : n.body.replace('\n', ' ');
            if (body.length() > 80) body = body.substring(0, 80) + "…";
            String tags = n.tags == null || n.tags.isEmpty() ? "" : "🏷 " + n.tags + "\n";
            r.subtitle.setText(tags + body);
            r.trailing.setText(c == null || c.code == null ? "" : c.code);
        });
        adapter.onClick(n -> startActivity(new Intent(this, NoteEditActivity.class).putExtra("id", n.id)));
        adapter.onLongClick(n -> UiUtil.confirm(this, "Delete note?", n.title, "Delete", () -> vm.delete(n)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No notes yet.\nTap + to write one.");

        vm.courses.observe(this, list -> {
            courseMap.clear();
            for (Course c : list) courseMap.put(c.id, c);
            adapter.refresh();
        });
        vm.notes.observe(this, list -> {
            adapter.submit(list);
            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        b.fab.setOnClickListener(v -> startActivity(new Intent(this, NoteEditActivity.class)));

        if (Prefs.notesLock(this) && !unlocked) {
            b.getRoot().setVisibility(View.INVISIBLE);
            BiometricGate.authenticate(this, "Unlock notes", () -> {
                unlocked = true;
                b.getRoot().setVisibility(View.VISIBLE);
            }, this::finish);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("unlocked", unlocked);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, 100, Menu.NONE, "Search");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        SearchView sv = new SearchView(this);
        sv.setQueryHint("Search title, text or #tag");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return true; }

            @Override
            public boolean onQueryTextChange(String q) {
                vm.query.setValue(q == null ? "" : q.replace("#", "").trim());
                return true;
            }
        });
        item.setActionView(sv);
        return true;
    }
}
