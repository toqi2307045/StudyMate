package com.example.studymate.docs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Writes one PDF page per image, applying rotation, page size, resolution and colour filter. */
public class ImagePdfExporter implements PdfExporter<List<ImagePage>> {
    private final ImageOptions options;

    public ImagePdfExporter(ImageOptions options) { this.options = options; }

    @Override
    public File export(Context context, List<ImagePage> pages, File out, Progress progress) throws IOException {
        PdfDocument doc = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(ImageFilters.filter(options.filter));
        try {
            for (int i = 0; i < pages.size(); i++) {
                ImagePage ip = pages.get(i);
                Bitmap bmp = ImageUtil.rotate(ImageUtil.decode(context, ip.uri, options.maxDim()), ip.rotation);
                int bw = bmp.getWidth(), bh = bmp.getHeight();
                int pw, ph;
                if (options.pageSize == 2) {
                    pw = 595;
                    ph = Math.max(1, Math.round(595f * bh / bw));
                } else {
                    pw = options.pageSize == 0 ? 595 : 612;
                    ph = options.pageSize == 0 ? 842 : 792;
                    if (bw > bh) { int t = pw; pw = ph; ph = t; }
                }
                PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(pw, ph, i + 1).create());
                Canvas c = page.getCanvas();
                c.drawColor(Color.WHITE);
                float scale = Math.min(pw / (float) bw, ph / (float) bh);
                float dw = bw * scale, dh = bh * scale;
                float left = (pw - dw) / 2f, top = (ph - dh) / 2f;
                c.drawBitmap(bmp, null, new RectF(left, top, left + dw, top + dh), paint);
                doc.finishPage(page);
                bmp.recycle();
                if (progress != null) progress.onProgress(i + 1, pages.size());
            }
            try (FileOutputStream os = new FileOutputStream(out)) {
                doc.writeTo(os);
            }
        } finally {
            doc.close();
        }
        return out;
    }
}
