package com.example.studymate.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.studymate.ui.PdfReaderActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Saves finished PDFs (merged / converted) into the "StudyMate" folder or a folder chosen in Settings. */
public final class PdfStorage {
    public static final String FOLDER = "StudyMate";
    private static final String KEY_TREE = "pdf_save_tree";

    private PdfStorage() {}

    public static class Saved {
        public final Uri uri;
        public final String where;

        Saved(Uri uri, String where) { this.uri = uri; this.where = where; }
    }

    // ---------- settings ----------
    private static Uri customTree(Context c) {
        String s = Prefs.getString(c, KEY_TREE, null);
        return s == null ? null : Uri.parse(s);
    }

    public static void setCustomTree(Context c, Uri tree) {
        try {
            c.getContentResolver().takePersistableUriPermission(tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) { }
        Prefs.putString(c, KEY_TREE, tree.toString());
    }

    public static void resetToDefault(Context c) { Prefs.remove(c, KEY_TREE); }

    /** Human-readable current save location. */
    public static String describe(Context c) {
        Uri t = customTree(c);
        return t != null && canWrite(c, t) ? treeLabel(t) : defaultLabel();
    }

    public static String defaultLabel() {
        return Build.VERSION.SDK_INT >= 29 ? "Downloads/" + FOLDER : "Documents/" + FOLDER + " (app storage)";
    }

    private static boolean canWrite(Context c, Uri tree) {
        for (UriPermission p : c.getContentResolver().getPersistedUriPermissions()) {
            if (p.getUri().equals(tree) && p.isWritePermission()) return true;
        }
        return false;
    }

    private static String treeLabel(Uri tree) {
        String id = DocumentsContract.getTreeDocumentId(tree); // e.g. "primary:Documents/StudyMate"
        int i = id.indexOf(':');
        String vol = i < 0 ? id : id.substring(0, i);
        String path = i < 0 ? "" : id.substring(i + 1);
        String root = vol.equals("primary") ? "Internal storage"
                : vol.matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}") ? "SD card" : "Selected folder";
        return path.isEmpty() ? root : root + "/" + path;
    }

    // ---------- saving ----------
    /** Copies a finished PDF into the save folder. Call off the main thread. */
    public static Saved save(Context c, File pdf) throws IOException {
        Uri tree = customTree(c);
        boolean fellBack = false;
        if (tree != null) {
            if (canWrite(c, tree)) {
                try {
                    return saveToTree(c, tree, pdf);
                } catch (IOException | RuntimeException ignored) { }
            }
            fellBack = true;
        }
        Saved s = saveToDefault(c, pdf);
        return fellBack ? new Saved(s.uri, s.where + "  (your chosen folder wasn't available)") : s;
    }

    private static Saved saveToTree(Context c, Uri tree, File pdf) throws IOException {
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
        Uri doc = DocumentsContract.createDocument(c.getContentResolver(), parent, "application/pdf", pdf.getName());
        if (doc == null) throw new IOException("Cannot create a file in the chosen folder");
        try {
            copy(c, pdf, doc);
        } catch (IOException | RuntimeException e) {
            try { DocumentsContract.deleteDocument(c.getContentResolver(), doc); } catch (Exception ignored) { }
            throw e;
        }
        return new Saved(doc, treeLabel(tree) + "/" + FileUtil.displayName(c, doc));
    }

    private static Saved saveToDefault(Context c, File pdf) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver r = c.getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, pdf.getName());
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER);
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IOException("Cannot create a file in Downloads");
            try {
                copy(c, pdf, uri);
                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                r.update(uri, done, null, null);
            } catch (IOException | RuntimeException e) {
                r.delete(uri, null, null);
                throw e;
            }
            return new Saved(uri, "Downloads/" + FOLDER + "/" + FileUtil.displayName(c, uri));
        }
        File base = c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (base == null) base = c.getFilesDir();
        File dir = new File(base, FOLDER);
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create folder " + FOLDER);
        File out = unique(dir, pdf.getName());
        try (InputStream in = new FileInputStream(pdf);
             OutputStream os = new java.io.FileOutputStream(out)) {
            pipe(in, os);
        }
        return new Saved(Uri.fromFile(out), defaultLabel() + "/" + out.getName());
    }

    private static File unique(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        String base = FileUtil.baseName(name), ext = FileUtil.ext(name);
        for (int i = 1; ; i++) {
            f = new File(dir, base + " (" + i + ")" + (ext.isEmpty() ? "" : "." + ext));
            if (!f.exists()) return f;
        }
    }

    private static void copy(Context c, File src, Uri dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream os = c.getContentResolver().openOutputStream(dst)) {
            if (os == null) throw new IOException("Cannot open destination");
            pipe(in, os);
        }
    }

    private static void pipe(InputStream in, OutputStream os) throws IOException {
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
    }

    /**
     * Call from a background thread once a PDF has been written to {@code pdf}:
     * saves it to the StudyMate folder, tells the user where it went, and opens it in the reader.
     */
    public static void deliver(Activity a, File pdf) {
        Uri open = FileUtil.shareUri(a, pdf);
        String msg;
        try {
            Saved s = save(a, pdf);
            open = s.uri;
            msg = "Saved to " + s.where;
        } catch (Exception e) {
            msg = "Couldn't save to the " + FOLDER + " folder (" + e.getMessage() + "). A copy is kept inside the app.";
        }
        final Uri uri = open;
        final String m = msg;
        AppExecutors.main(() -> {
            Toast.makeText(a, m, Toast.LENGTH_LONG).show();
            a.startActivity(new Intent(a, PdfReaderActivity.class)
                    .putExtra("uri", uri.toString()).putExtra("name", pdf.getName()));
        });
    }
}