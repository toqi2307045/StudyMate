package com.example.studymate.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.CourseFile;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.util.FileUtil;
import com.example.studymate.viewmodel.FilesViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Attach PDF / PPTX / DOCX files to a course and browse them by category. */
public class CourseFilesActivity extends BaseActivity {
    private static final String[] CATEGORIES = {"Lecture", "Past paper", "Other"};

    private ActivityListBinding b;
    private FilesViewModel vm;
    private List<Course> courses = new ArrayList<>();
    private List<CourseFile> allFiles = new ArrayList<>();
    private int categoryFilter = 0; // 0 = all, else 1..3 -> CATEGORIES[i-1]
    private RowAdapter<CourseFile> adapter;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Course Files", true);
        vm = new ViewModelProvider(this).get(FilesViewModel.class);

        Spinner courseSpinner = new Spinner(this);
        b.headerContainer.addView(courseSpinner);
        b.headerContainer.addView(UiUtil.segmented(this, new String[]{"All", "Lectures", "Past papers", "Other"}, 0, i -> {
            categoryFilter = i;
            applyFilter();
        }));

        courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos >= 0 && pos < courses.size()) vm.courseId.setValue(courses.get(pos).id);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        vm.courses.observe(this, list -> {
            courses = list;
            List<String> names = new ArrayList<>();
            int sel = 0;
            long cur = vm.courseId.getValue() == null ? 0 : vm.courseId.getValue();
            for (int i = 0; i < list.size(); i++) {
                names.add(list.get(i).label());
                if (list.get(i).id == cur) sel = i;
            }
            ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            courseSpinner.setAdapter(ad);
            if (!names.isEmpty()) courseSpinner.setSelection(sel);
            applyFilter();
        });

        adapter = new RowAdapter<>((r, f) -> {
            r.strip.setBackgroundColor(f.type.equals("pdf") ? 0xFFE53935 : f.type.equals("pptx") ? 0xFFFF9800 : 0xFF1E88E5);
            r.title.setText(f.name);
            r.subtitle.setText(f.category);
            r.trailing.setText(f.type.toUpperCase());
        });
        adapter.onClick(f -> Openers.open(this, f.uri, f.name, f.type));
        adapter.onLongClick(f -> UiUtil.confirm(this, "Remove file?", "Removes it from this course (the file itself is not deleted).",
                "Remove", () -> vm.deleteFile(f)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);

        vm.files.observe(this, list -> { allFiles = list; applyFilter(); });
        b.fab.setOnClickListener(v -> {
            if (courses.isEmpty()) { UiUtil.toast(this, "Add a course first"); return; }
            picker.launch(new String[]{"application/pdf", Openers.MIME_PPTX, Openers.MIME_DOCX, "application/octet-stream"});
        });
    }

    private void applyFilter() {
        List<CourseFile> out = new ArrayList<>();
        for (CourseFile f : allFiles) {
            if (categoryFilter == 0 || f.category.equals(CATEGORIES[categoryFilter - 1])) out.add(f);
        }
        adapter.submit(out);
        b.empty.setText(courses.isEmpty() ? "Add a course first, then attach slides and past papers."
                : "No files here yet.\nTap + to attach a PDF, PPTX or DOCX.");
        b.empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onPicked(Uri uri) {
        if (uri == null) return;
        String name = FileUtil.displayName(this, uri);
        String ext = FileUtil.ext(name);
        if (!(ext.equals("pdf") || ext.equals("pptx") || ext.equals("docx"))) {
            UiUtil.toast(this, "Only PDF, PPTX and DOCX files are supported");
            return;
        }
        FileUtil.takePersist(this, uri);
        FormDialog f = new FormDialog(this);
        EditText label = f.text("Display name", FileUtil.baseName(name));
        Spinner cat = f.spinner("Category", Arrays.asList(CATEGORIES), 0);
        f.show("Attach file", "Attach", () -> {
            CourseFile cf = new CourseFile();
            cf.courseId = vm.courseId.getValue() == null ? 0 : vm.courseId.getValue();
            cf.name = FormDialog.str(label).isEmpty() ? name : FormDialog.str(label);
            cf.uri = uri.toString();
            cf.type = ext;
            cf.category = CATEGORIES[cat.getSelectedItemPosition()];
            cf.addedAt = System.currentTimeMillis();
            vm.addFile(cf);
            return true;
        });
    }
}
