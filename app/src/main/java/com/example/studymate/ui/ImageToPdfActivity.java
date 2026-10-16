package com.example.studymate.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.databinding.ActivityImagePdfBinding;
import com.example.studymate.databinding.ItemImagePageBinding;
import com.example.studymate.docs.ExporterFactory;
import com.example.studymate.docs.ImageFilters;
import com.example.studymate.docs.ImageOptions;
import com.example.studymate.docs.ImagePage;
import com.example.studymate.docs.ImageUtil;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.FileUtil;
import com.example.studymate.viewmodel.ImageToPdfViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageToPdfActivity extends BaseActivity {
    private static final int M_OPTIONS = 1, M_CLEAR = 2;

    private ActivityImagePdfBinding b;
    private ImageToPdfViewModel vm;
    private PageAdapter adapter;
    private final android.util.LruCache<String, Bitmap> thumbs = new android.util.LruCache<>(40);

    private final ActivityResultLauncher<String> gallery =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null) return;
                for (Uri u : uris) vm.pages.add(new ImagePage(u));
                refresh();
            });

    private final ActivityResultLauncher<Uri> camera =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
                if (Boolean.TRUE.equals(ok) && vm.cameraUri != null) {
                    vm.pages.add(new ImagePage(vm.cameraUri));
                    refresh();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityImagePdfBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Image → PDF", true);
        vm = new ViewModelProvider(this).get(ImageToPdfViewModel.class);

        adapter = new PageAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                int a = from.getBindingAdapterPosition(), t = to.getBindingAdapterPosition();
                ImagePage p = vm.pages.remove(a);
                vm.pages.add(t, p);
                adapter.notifyItemMoved(a, t);
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                adapter.notifyDataSetChanged(); // refresh page numbers
            }
        }).attachToRecyclerView(b.recycler);

        b.btnGallery.setOnClickListener(v -> gallery.launch("image/*"));
        b.btnCamera.setOnClickListener(v -> takePhoto());
        b.btnExport.setOnClickListener(v -> export());
        b.optionsSummary.setOnClickListener(v -> showOptions());
        refresh();
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        b.empty.setVisibility(vm.pages.isEmpty() ? View.VISIBLE : View.GONE);
        b.optionsSummary.setText("Pages: " + vm.pages.size() + "   ·   " + vm.options.summary() + "  (tap to change)");
    }

    private void takePhoto() {
        try {
            File dir = new File(getCacheDir(), "camera");
            if (!dir.exists()) dir.mkdirs();
            File f = File.createTempFile("scan_", ".jpg", dir);
            vm.cameraUri = FileUtil.shareUri(this, f);
            camera.launch(vm.cameraUri);
        } catch (ActivityNotFoundException e) {
            UiUtil.toast(this, "No camera app available");
        } catch (Exception e) {
            UiUtil.toast(this, "Cannot start camera: " + e.getMessage());
        }
    }

    private void showOptions() {
        FormDialog f = new FormDialog(this);
        Spinner size = f.spinner("Page size", Arrays.asList(ImageOptions.PAGE_SIZES), vm.options.pageSize);
        Spinner quality = f.spinner("Quality (resolution)", Arrays.asList(ImageOptions.QUALITIES), vm.options.quality);
        Spinner filter = f.spinner("Filter (scan mode)", Arrays.asList(ImageOptions.FILTERS), vm.options.filter);
        f.show("Export options", "Apply", () -> {
            vm.options.pageSize = size.getSelectedItemPosition();
            vm.options.quality = quality.getSelectedItemPosition();
            vm.options.filter = filter.getSelectedItemPosition();
            refresh();
            return true;
        });
    }

    private void export() {
        if (vm.pages.isEmpty()) { UiUtil.toast(this, "Add at least one image"); return; }
        List<ImagePage> snapshot = new ArrayList<>();
        for (ImagePage p : vm.pages) {
            ImagePage c = new ImagePage(p.uri);
            c.rotation = p.rotation;
            snapshot.add(c);
        }
        ImageOptions opts = new ImageOptions();
        opts.pageSize = vm.options.pageSize;
        opts.quality = vm.options.quality;
        opts.filter = vm.options.filter;

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(snapshot.size());
        pb.setPadding(UiUtil.dp(this, 24), UiUtil.dp(this, 16), UiUtil.dp(this, 24), 0);
        AlertDialog dlg = new MaterialAlertDialogBuilder(this).setTitle("Creating PDF...").setView(pb)
                .setCancelable(false).show();

        AppExecutors.io(() -> {
            try {
                File out = new File(FileUtil.convertedDir(this), "images_" + System.currentTimeMillis() + ".pdf");
                ExporterFactory.images(opts).export(this, snapshot, out,
                        (done, total) -> AppExecutors.main(() -> pb.setProgress(done)));
                AppExecutors.main(() -> {
                    dlg.dismiss();
                    UiUtil.toast(this, "PDF saved");
                    startActivity(new Intent(this, PdfReaderActivity.class)
                            .putExtra("uri", FileUtil.shareUri(this, out).toString())
                            .putExtra("name", out.getName()));
                });
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    dlg.dismiss();
                    UiUtil.toast(this, "Export failed: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_OPTIONS, "Export options");
        addMenu(menu, M_CLEAR, "Remove all pages");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_OPTIONS) { showOptions(); return true; }
        if (item.getItemId() == M_CLEAR) {
            UiUtil.confirm(this, "Remove all pages?", "The source images are not deleted.", "Remove", () -> {
                vm.pages.clear();
                refresh();
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class PageAdapter extends RecyclerView.Adapter<PageAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final ItemImagePageBinding ib;

            VH(ItemImagePageBinding ib) { super(ib.getRoot()); this.ib = ib; }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemImagePageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ImagePage page = vm.pages.get(position);
            h.ib.pageNumber.setText(String.valueOf(position + 1));
            h.ib.imageInfo.setText(page.rotation == 0 ? "Long-press to drag" : "Rotated " + page.rotation + "°");
            h.ib.imageThumb.setRotation(page.rotation);
            h.ib.imageThumb.setColorFilter(ImageFilters.filter(vm.options.filter));
            String key = page.uri.toString();
            Bitmap cached = thumbs.get(key);
            if (cached != null) {
                h.ib.imageThumb.setImageBitmap(cached);
            } else {
                h.ib.imageThumb.setImageBitmap(null);
                AppExecutors.io(() -> {
                    try {
                        Bitmap bm = ImageUtil.decode(ImageToPdfActivity.this, page.uri, 300);
                        thumbs.put(key, bm);
                        AppExecutors.main(() -> notifyDataSetChanged());
                    } catch (Exception ignored) { }
                });
            }
            h.ib.btnRotate.setOnClickListener(v -> {
                page.rotation = (page.rotation + 90) % 360;
                int pos = h.getBindingAdapterPosition();
                if (pos >= 0) notifyItemChanged(pos);
            });
            h.ib.btnDelete.setOnClickListener(v -> {
                int pos = h.getBindingAdapterPosition();
                if (pos < 0) return;
                vm.pages.remove(pos);
                notifyItemRemoved(pos);
                refresh();
            });
        }

        @Override
        public int getItemCount() { return vm.pages.size(); }
    }
}
