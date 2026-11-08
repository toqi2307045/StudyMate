package com.example.studymate.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import com.example.studymate.docs.ImageUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Stores contact photos as small square JPEGs in app storage and makes Firestore-sized thumbnails. */
public final class ContactPhotos {
    private ContactPhotos() {}

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "contact_photos");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** Center-crops the picked image to a square (max 512 px) and saves it. Call off the main thread. */
    public static String saveFromUri(Context c, String syncId, Uri src) throws IOException {
        Bitmap b = ImageUtil.decode(c, src, 512);
        int s = Math.min(b.getWidth(), b.getHeight());
        Bitmap sq = Bitmap.createBitmap(b, (b.getWidth() - s) / 2, (b.getHeight() - s) / 2, s, s);
        if (sq != b) b.recycle();
        File out = new File(dir(c), syncId + ".jpg");
        try (FileOutputStream os = new FileOutputStream(out)) {
            sq.compress(Bitmap.CompressFormat.JPEG, 85, os);
        }
        sq.recycle();
        return out.getAbsolutePath();
    }

    /** Writes a thumbnail received from Firestore to disk. Returns null on failure. */
    public static String saveFromBase64(Context c, String syncId, String b64) {
        try {
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            File out = new File(dir(c), syncId + ".jpg");
            try (FileOutputStream os = new FileOutputStream(out)) {
                os.write(data);
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** ~240 px JPEG as base64 (roughly 10-20 KB) so a Firestore document stays far below the 1 MiB limit. */
    public static String thumbBase64(String path) {
        Bitmap b = load(path, 240);
        if (b == null) return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 75, bos);
        b.recycle();
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }

    /** Decodes a saved photo, downsampled so its long side is at least maxDim/2..maxDim. */
    public static Bitmap load(String path, int maxDim) {
        if (path == null || !new File(path).exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int big = Math.max(o.outWidth, o.outHeight);
        int sample = 1;
        while (big / (sample * 2) >= maxDim) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, o);
    }

    public static void delete(String path) {
        if (path != null) new File(path).delete();
    }
}
