package com.example.studymate.docs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

public final class ImageUtil {
    private ImageUtil() {}

    /** Decodes an image downsampled to at most maxDim on its long side, honouring EXIF rotation. */
    public static Bitmap decode(Context c, Uri uri, int maxDim) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        int big = Math.max(o.outWidth, o.outHeight);
        int sample = 1;
        while (big / (sample * 2) >= maxDim) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, o);
        }
        if (bmp == null) throw new IOException("Cannot decode image");

        int deg = 0;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (ori == ExifInterface.ORIENTATION_ROTATE_90) deg = 90;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_180) deg = 180;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_270) deg = 270;
            }
        } catch (Exception ignored) { }
        return rotate(bmp, deg);
    }

    public static Bitmap rotate(Bitmap src, int degrees) {
        if (degrees % 360 == 0) return src;
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap out = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        if (out != src) src.recycle();
        return out;
    }
}
