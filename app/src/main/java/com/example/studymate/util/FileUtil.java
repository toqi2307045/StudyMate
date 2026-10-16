package com.example.studymate.util;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FileUtil {
    private FileUtil() {}

    public static String displayName(Context c, Uri uri) {
        String name = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cur = c.getContentResolver().query(uri, null, null, null, null)) {
                if (cur != null && cur.moveToFirst()) {
                    int i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i >= 0) name = cur.getString(i);
                }
            } catch (Exception ignored) { }
        }
        if (name == null) {
            String p = uri.getLastPathSegment();
            name = p == null ? "file" : p.substring(p.lastIndexOf('/') + 1);
        }
        return name;
    }

    public static String ext(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    public static String baseName(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }

    public static void takePersist(Context c, Uri uri) {
        try {
            c.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
    }

    /** Copies any readable Uri into the cache so zip / PdfRenderer can use a real File. */
    public static File copyToCache(Context c, Uri uri, String ext) throws IOException {
        File dir = new File(c.getCacheDir(), "docs");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, Math.abs(uri.toString().hashCode()) + "." + ext);
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("Cannot open file");
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        }
        return out;
    }

    public static File convertedDir(Context c) {
        File d = new File(c.getFilesDir(), "converted");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static Uri shareUri(Context c, File f) {
        return FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", f);
    }
}
