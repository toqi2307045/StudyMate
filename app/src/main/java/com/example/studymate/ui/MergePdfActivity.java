package com.example.studymate.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studymate.R;
import com.example.studymate.util.AppExecutors;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.IOException;

public class MergePdfActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.size() < 2) {
                    Toast.makeText(this, "Select at least 2 PDFs", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                AppExecutors.io(() -> {
                    try {
                        File out = new File(getExternalFilesDir(null), "merged_" + System.currentTimeMillis() + ".pdf");
                        PdfMerger.merge(this, uris, out);
                        runOnUiThread(() -> {
                            Intent i = new Intent(this, PdfReaderActivity.class);
                            i.setData(Uri.fromFile(out));
                            startActivity(i);
                            finish();
                        });
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Merge failed", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });
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

        picker.launch(new String[]{"application/pdf"});
    }
}
