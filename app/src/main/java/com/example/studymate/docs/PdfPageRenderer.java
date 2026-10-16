package com.example.studymate.docs;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/** Thread-safe wrapper over android.graphics.pdf.PdfRenderer (only one page may be open at a time). */
public class PdfPageRenderer implements Closeable {
    private final ParcelFileDescriptor pfd;
    private final PdfRenderer renderer;

    public PdfPageRenderer(File file) throws IOException {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        try {
            renderer = new PdfRenderer(pfd);
        } catch (IOException | RuntimeException e) {
            pfd.close();
            throw e;
        }
    }

    public int pageCount() { return renderer.getPageCount(); }

    public synchronized Bitmap render(int index, int widthPx) {
        PdfRenderer.Page page = renderer.openPage(index);
        try {
            float ratio = page.getHeight() / (float) page.getWidth();
            Bitmap bmp = Bitmap.createBitmap(widthPx, Math.max(1, Math.round(widthPx * ratio)), Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bmp;
        } finally {
            page.close();
        }
    }

    @Override
    public synchronized void close() {
        try {
            renderer.close();
            pfd.close();
        } catch (IOException ignored) { }
    }
}
