package com.example.studymate.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studymate.databinding.ActivityPptxViewerBinding;
import com.example.studymate.databinding.ItemSlideBinding;
import com.example.studymate.docs.ExporterFactory;
import com.example.studymate.docs.ParserFactory;
import com.example.studymate.docs.PptxModel;
import com.example.studymate.docs.SlideRenderer;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.FileUtil;
import com.example.studymate.util.PdfStorage;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class PptxViewerActivity extends BaseActivity {
    private static final int M_PDF = 1, M_EXTERNAL = 2;

    private ActivityPptxViewerBinding b;
    private PptxModel model;
    private Uri uri;
    private String name;
    private int slideWidth;
    private final LruCache<Integer, Bitmap> cache = new LruCache<>(6);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPptxViewerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        String u = getIntent().getStringExtra("uri");
        if (u == null) { finish(); return; }
        uri = Uri.parse(u);
        name = getIntent().getStringExtra("name");
        if (name == null) name = FileUtil.displayName(this, uri);
        setupToolbar(b.toolbar, name, true);
        slideWidth = getResources().getDisplayMetrics().widthPixels;
        boolean autoConvert = getIntent().getBooleanExtra("autoConvert", false);

        AppExecutors.io(() -> {
            try {
                File f = FileUtil.copyToCache(this, uri, "pptx");
                PptxModel m = ParserFactory.pptx().parse(f);
                AppExecutors.main(() -> {
                    if (isDestroyed()) { m.close(); return; }
                    model = m;
                    showSlides();
                    if (autoConvert) convertToPdf();
                });
            } catch (Exception e) {
                AppExecutors.main(() -> showFallback(e.getMessage()));
            }
        });
    }

    private void showSlides() {
        b.loading.setVisibility(View.GONE);
        b.pager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ItemSlideBinding sb = ItemSlideBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new RecyclerView.ViewHolder(sb.getRoot()) { };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                ItemSlideBinding sb = ItemSlideBinding.bind(h.itemView);
                PptxModel.Slide s = model.slides.get(pos);
                String text = s.plainText();
                sb.slideText.setText(text);
                sb.slideText.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
                sb.notesText.setText(s.notes == null || s.notes.trim().isEmpty() ? "No speaker notes for this slide." : s.notes);
                Bitmap cached = cache.get(pos);
                if (cached != null) {
                    sb.slideImage.setImageBitmap(cached);
                    return;
                }
                sb.slideImage.setImageBitmap(null);
                AppExecutors.io(() -> {
                    try {
                        Bitmap bm = SlideRenderer.renderBitmap(model, s, slideWidth);
                        cache.put(pos, bm);
                        AppExecutors.main(() -> {
                            if (h.getBindingAdapterPosition() == pos) sb.slideImage.setImageBitmap(bm);
                        });
                    } catch (Exception ignored) { }
                });
            }

            @Override
            public int getItemCount() { return model.slides.size(); }
        });
        updateSubtitle(0);
        b.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { updateSubtitle(position); }
        });
    }

    private void updateSubtitle(int pos) {
        if (getSupportActionBar() != null && model != null)
            getSupportActionBar().setSubtitle("Slide " + (pos + 1) + " of " + model.slides.size());
    }

    private void convertToPdf() {
        if (model == null) return;
        UiUtil.toast(this, "Converting to PDF...");
        AppExecutors.io(() -> {
            try {
                File out = new File(FileUtil.convertedDir(this), FileUtil.baseName(name) + ".pdf");
                ExporterFactory.slides().export(this, model, out, null);
                PdfStorage.deliver(this, out);
            } catch (Exception e) {
                AppExecutors.main(() -> UiUtil.toast(this, "Conversion failed: " + e.getMessage()));
            }
        });
    }

    private void openExternal() {
        Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, Openers.MIME_PPTX)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Open with"));
        } catch (ActivityNotFoundException e) {
            UiUtil.toast(this, "No app installed that can open PPTX files");
        }
    }

    private void showFallback(String reason) {
        b.loading.setVisibility(View.GONE);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Can't display this presentation")
                .setMessage("The built-in viewer could not read this file (" + reason + "). Open it in another app instead?")
                .setPositiveButton("Open in another app", (d, w) -> { openExternal(); finish(); })
                .setNegativeButton("Close", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_PDF, "Convert to PDF");
        addMenu(menu, M_EXTERNAL, "Open in another app");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_PDF) { convertToPdf(); return true; }
        if (item.getItemId() == M_EXTERNAL) { openExternal(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (model != null) model.close();
    }
}
