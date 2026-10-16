package com.example.studymate.docs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** In-memory representation of a .pptx: slide shapes with text, positions and image references. */
public class PptxModel implements Closeable {

    public static class Para {
        public String text = "";
        public boolean bold;
        public boolean bullet;
        public boolean noBullet;
        public float sizePt; // 0 = use default
    }

    public static class Shape {
        public boolean image;
        public String placeholder;      // title, ctrTitle, subTitle, body, ...
        public long x = -1, y = -1, w = -1, h = -1; // EMU, -1 = inherited from layout (unknown)
        public final List<Para> paras = new ArrayList<>();
        public String embedId;
        public String mediaPath;

        public boolean isTitle() {
            return "title".equals(placeholder) || "ctrTitle".equals(placeholder);
        }
    }

    public static class Slide {
        public final List<Shape> shapes = new ArrayList<>();
        public String notes = "";

        public String plainText() {
            StringBuilder sb = new StringBuilder();
            for (Shape s : shapes) {
                for (Para p : s.paras) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(p.bullet ? "• " : "").append(p.text);
                }
            }
            return sb.toString();
        }
    }

    public long widthEmu = 9144000L;
    public long heightEmu = 5143500L;
    public final List<Slide> slides = new ArrayList<>();
    ZipFile zip;

    public Bitmap loadImage(String path, int maxDim) {
        if (zip == null || path == null) return null;
        try {
            ZipEntry e = zip.getEntry(path);
            if (e == null) return null;
            byte[] data;
            try (InputStream in = zip.getInputStream(e)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                data = bos.toByteArray();
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int big = Math.max(o.outWidth, o.outHeight);
            int sample = 1;
            while (big / (sample * 2) >= maxDim) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    @Override
    public void close() {
        try {
            if (zip != null) zip.close();
        } catch (IOException ignored) { }
    }
}
