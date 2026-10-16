package com.example.studymate.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Bookmark;
import com.example.studymate.databinding.ActivityPdfReaderBinding;
import com.example.studymate.databinding.ItemThumbBinding;
import com.example.studymate.docs.PdfPageRenderer;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.FileUtil;
import com.example.studymate.util.Prefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfReaderActivity extends BaseActivity {
    private static final int M_ADD_BM = 1, M_BMS = 2, M_THUMBS = 3, M_GOTO = 4, M_NIGHT = 5, M_SHARE = 6;

    private ActivityPdfReaderBinding b;
    private Repository repo;
    private PdfPageRenderer renderer;
    private File cacheFile;
    private String uriStr, name;
    private int page = 0, count = 0, renderWidth;
    private boolean night;
    private final ExecutorService renderExec = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPdfReaderBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        repo = Repository.get(this);
        uriStr = getIntent().getStringExtra("uri");
        name = getIntent().getStringExtra("name");
        if (uriStr == null) { finish(); return; }
        if (name == null) name = "PDF";
        setupToolbar(b.toolbar, name, true);
        night = Prefs.pdfNight(this);
        applyNight();
        renderWidth = Math.min(2400, getResources().getDisplayMetrics().widthPixels * 2);

        b.pageView.swipeListener = next -> showPage(next ? page + 1 : page - 1);
        b.btnPrev.setOnClickListener(v -> showPage(page - 1));
        b.btnNext.setOnClickListener(v -> showPage(page + 1));
        b.pageLabel.setOnClickListener(v -> askPage());

        AppExecutors.io(() -> {
            try {
                cacheFile = FileUtil.copyToCache(this, Uri.parse(uriStr), "pdf");
                PdfPageRenderer r = new PdfPageRenderer(cacheFile);
                repo.findRecent(uriStr, rf -> {
                    int start = rf == null ? 0 : Math.max(0, Math.min(rf.lastPage, r.pageCount() - 1));
                    onLoaded(r, start);
                });
            } catch (SecurityException e) {
                fail("This PDF is password protected and can't be opened here.");
            } catch (Exception e) {
                fail("Couldn't open this PDF: " + e.getMessage());
            }
        });
    }

    private void fail(String msg) {
        AppExecutors.main(() -> { UiUtil.toast(this, msg); finish(); });
    }

    private void onLoaded(PdfPageRenderer r, int start) {
        if (isDestroyed()) { r.close(); return; }
        renderer = r;
        count = r.pageCount();
        b.loading.setVisibility(View.GONE);
        showPage(start);
    }

    private void applyNight() {
        if (night) {
            ColorMatrix inv = new ColorMatrix(new float[]{
                    -1, 0, 0, 0, 255,
                    0, -1, 0, 0, 255,
                    0, 0, -1, 0, 255,
                    0, 0, 0, 1, 0});
            b.pageView.setColorFilter(new ColorMatrixColorFilter(inv));
            b.pageHolder.setBackgroundColor(0xFF000000);
        } else {
            b.pageView.clearColorFilter();
            b.pageHolder.setBackgroundColor(0xFF808080);
        }
    }

    private void showPage(int p) {
        if (renderer == null || count == 0) return;
        p = Math.max(0, Math.min(count - 1, p));
        page = p;
        b.pageLabel.setText((p + 1) + " / " + count);
        repo.upsertRecent(uriStr, name, p, count);
        final int target = p;
        renderExec.execute(() -> {
            try {
                Bitmap bm = renderer.render(target, renderWidth);
                AppExecutors.main(() -> {
                    if (target == page && !isDestroyed()) b.pageView.setImageBitmap(bm);
                });
            } catch (Exception ignored) { }
        });
    }

    private void askPage() {
        if (count == 0) return;
        FormDialog f = new FormDialog(this);
        EditText e = f.number("Go to page (1-" + count + ")", String.valueOf(page + 1));
        f.show("Go to page", "Go", () -> {
            try {
                int n = (int) Double.parseDouble(FormDialog.str(e));
                if (n < 1 || n > count) throw new NumberFormatException();
                showPage(n - 1);
                return true;
            } catch (NumberFormatException ex) {
                e.setError("Enter 1-" + count);
                return false;
            }
        });
    }

    private void showBookmarks() {
        repo.bookmarks(uriStr, list -> {
            MaterialAlertDialogBuilder d = new MaterialAlertDialogBuilder(this).setTitle("Bookmarks");
            if (list.isEmpty()) {
                d.setMessage("No bookmarks yet. Use \"Bookmark this page\" in the menu.");
                d.setPositiveButton("OK", null);
                d.show();
                return;
            }
            String[] rows = new String[list.size()];
            for (int i = 0; i < rows.length; i++) rows[i] = "Page " + (list.get(i).page + 1);
            d.setItems(rows, (dlg, i) -> showPage(list.get(i).page));
            d.setNegativeButton("Close", null);
            AlertDialog dialog = d.create();
            dialog.show();
            dialog.getListView().setOnItemLongClickListener((parent, view, pos, id) -> {
                Bookmark bm = list.get(pos);
                repo.deleteBookmark(bm);
                dialog.dismiss();
                UiUtil.toast(this, "Bookmark removed");
                return true;
            });
        });
    }

    private void showThumbnails() {
        if (renderer == null) return;
        ExecutorService thumbExec = Executors.newSingleThreadExecutor();
        android.util.LruCache<Integer, Bitmap> cache = new android.util.LruCache<>(60);
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiUtil.dp(this, 440)));
        AlertDialog[] holder = new AlertDialog[1];
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ItemThumbBinding tb = ItemThumbBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new RecyclerView.ViewHolder(tb.getRoot()) { };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                ItemThumbBinding tb = ItemThumbBinding.bind(h.itemView);
                tb.thumbNum.setText(String.valueOf(pos + 1));
                tb.getRoot().setOnClickListener(v -> { holder[0].dismiss(); showPage(pos); });
                Bitmap cached = cache.get(pos);
                if (cached != null) { tb.thumb.setImageBitmap(cached); return; }
                tb.thumb.setImageBitmap(null);
                try {
                    thumbExec.execute(() -> {
                        try {
                            Bitmap bm = renderer.render(pos, 260);
                            cache.put(pos, bm);
                            AppExecutors.main(() -> {
                                if (h.getBindingAdapterPosition() == pos) tb.thumb.setImageBitmap(bm);
                            });
                        } catch (Exception ignored) { }
                    });
                } catch (RuntimeException ignored) { }
            }

            @Override
            public int getItemCount() { return count; }
        });
        holder[0] = new MaterialAlertDialogBuilder(this).setTitle("Pages").setView(rv)
                .setNegativeButton("Close", null)
                .setOnDismissListener(d -> thumbExec.shutdownNow())
                .create();
        holder[0].show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_ADD_BM, "Bookmark this page");
        addMenu(menu, M_BMS, "Bookmarks");
        addMenu(menu, M_THUMBS, "Page thumbnails");
        addMenu(menu, M_GOTO, "Go to page");
        MenuItem n = addMenu(menu, M_NIGHT, "Night mode");
        n.setCheckable(true);
        n.setChecked(night);
        addMenu(menu, M_SHARE, "Share / save a copy");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case M_ADD_BM:
                repo.addBookmark(uriStr, page);
                UiUtil.toast(this, "Bookmarked page " + (page + 1));
                return true;
            case M_BMS: showBookmarks(); return true;
            case M_THUMBS: showThumbnails(); return true;
            case M_GOTO: askPage(); return true;
            case M_NIGHT:
                night = !night;
                Prefs.putBool(this, "pdf_night", night);
                item.setChecked(night);
                applyNight();
                return true;
            case M_SHARE:
                if (cacheFile != null) {
                    Intent i = new Intent(Intent.ACTION_SEND).setType("application/pdf")
                            .putExtra(Intent.EXTRA_STREAM, FileUtil.shareUri(this, cacheFile))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(i, "Share PDF"));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        final PdfPageRenderer r = renderer;
        renderExec.execute(() -> { if (r != null) r.close(); });
        renderExec.shutdown();
    }
}
