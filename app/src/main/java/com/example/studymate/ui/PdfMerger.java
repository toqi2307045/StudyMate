package com.example.studymate.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfMerger {
    public static File merge(Context ctx, List<Uri> uris, File out) throws IOException {
        PdfDocument doc = new PdfDocument();
        int pageNo = 1;
        for (Uri uri : uris) {
            ParcelFileDescriptor pfd = ctx.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) continue;
            PdfRenderer r = new PdfRenderer(pfd);
            try {
                for (int i = 0; i < r.getPageCount(); i++) {
                    PdfRenderer.Page p = r.openPage(i);
                    int w = p.getWidth(), h = p.getHeight();
                    Bitmap bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    p.close();

                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(w, h, pageNo++).create();
                    PdfDocument.Page page = doc.startPage(info);
                    page.getCanvas().drawBitmap(bmp, null, new Rect(0, 0, w, h), null);
                    doc.finishPage(page);
                    bmp.recycle();
                }
            } finally {
                r.close();
                pfd.close();
            }
        }
        try (FileOutputStream fos = new FileOutputStream(out)) { doc.writeTo(fos); }
        doc.close();
        return out;
    }
}