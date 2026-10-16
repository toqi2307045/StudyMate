package com.example.studymate.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studymate.R;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.FileUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MergePdfActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.size() < 2) {
                    Toast.makeText(this, "Select at least 2 PDFs", Toast.LENGTH_SHORT).show();
                    return;
                }
                processMerge(uris);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("Merge PDFs");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        LinearLayout content = findViewById(R.id.content);
        if (content != null) {
            content.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView tvDesc = new TextView(this);
            tvDesc.setText("Select two or more PDF files to combine them into a single document.");
            tvDesc.setTextSize(16);
            tvDesc.setPadding(0, 32, 0, 32);
            tvDesc.setGravity(Gravity.CENTER);
            content.addView(tvDesc);

            MaterialButton btnSelect = new MaterialButton(this);
            btnSelect.setText("Select PDFs to Merge");
            btnSelect.setOnClickListener(v -> picker.launch(new String[]{"application/pdf"}));
            content.addView(btnSelect);
        }

        if (savedInstanceState == null) {
            picker.launch(new String[]{"application/pdf"});
        }
    }

    private void processMerge(List<Uri> uris) {
        ProgressBar pb = new ProgressBar(this);
        pb.setPadding(UiUtil.dp(this, 24), UiUtil.dp(this, 16), UiUtil.dp(this, 24), 0);
        AlertDialog dlg = new MaterialAlertDialogBuilder(this)
                .setTitle("Merging PDFs...")
                .setView(pb)
                .setCancelable(false)
                .show();

        AppExecutors.io(() -> {
            try {
                File out = new File(FileUtil.convertedDir(this), "merged_" + System.currentTimeMillis() + ".pdf");
                PdfMerger.merge(this, uris, out);
                runOnUiThread(() -> {
                    dlg.dismiss();
                    Toast.makeText(this, "PDFs merged successfully", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, PdfReaderActivity.class);
                    i.putExtra("uri", Uri.fromFile(out).toString());
                    i.putExtra("name", out.getName());
                    startActivity(i);
                    finish();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dlg.dismiss();
                    Toast.makeText(this, "Merge failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
