package com.example.studymate.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studymate.data.entity.RecentFile;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.util.FileUtil;
import com.example.studymate.viewmodel.FilesViewModel;

/** Recent PDFs + "open file" entry point for the PDF reader. */
public class PdfLibraryActivity extends BaseActivity {
    private ActivityListBinding b;
    private FilesViewModel vm;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                FileUtil.takePersist(this, uri);
                startActivity(new Intent(this, PdfReaderActivity.class)
                        .putExtra("uri", uri.toString()).putExtra("name", FileUtil.displayName(this, uri)));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "PDF Reader", true);
        vm = new ViewModelProvider(this).get(FilesViewModel.class);

        RowAdapter<RecentFile> adapter = new RowAdapter<>((r, f) -> {
            r.strip.setBackgroundColor(0xFFE53935);
            r.title.setText(f.name);
            r.subtitle.setText("Page " + (f.lastPage + 1) + " of " + f.pageCount);
            r.trailing.setText("PDF");
        });
        adapter.onClick(f -> startActivity(new Intent(this, PdfReaderActivity.class)
                .putExtra("uri", f.uri).putExtra("name", f.name)));
        adapter.onLongClick(f -> UiUtil.confirm(this, "Remove from recent?", f.name, "Remove", () -> vm.deleteRecent(f)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No recent PDFs.\nTap + to open one.");
        vm.recent.observe(this, list -> {
            adapter.submit(list);
            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        b.fab.setOnClickListener(v -> picker.launch(new String[]{"application/pdf"}));
    }
}
