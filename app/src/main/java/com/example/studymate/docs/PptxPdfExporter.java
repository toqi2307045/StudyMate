package com.example.studymate.docs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Renders every slide as high-res bitmap and exports to PDF page. */
public class PptxPdfExporter implements PdfExporter<PptxModel> {

    @Override
    public File export(Context context, PptxModel model, File out, Progress progress) throws IOException {
        if (model == null || model.slides.isEmpty()) {
            throw new IOException("Presentation contains no slides");
        }

        int targetW = 1920;

        // PDF page dimensions in points (72 DPI standard PDF points)
        int pdfW = Math.max(1, Math.round(model.widthEmu / 12700f));
        int pdfH = Math.max(1, Math.round(model.heightEmu / 12700f));

        PdfDocument doc = new PdfDocument();
        try {
            int total = model.slides.size();
            for (int i = 0; i < total; i++) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pdfW, pdfH, i + 1).create();
                PdfDocument.Page page = doc.startPage(info);

                Bitmap bmp = SlideRenderer.renderBitmap(model, model.slides.get(i), targetW);
                if (bmp != null) {
                    page.getCanvas().drawBitmap(bmp, null, new RectF(0, 0, pdfW, pdfH), null);
                    bmp.recycle();
                }

                doc.finishPage(page);
                if (progress != null) progress.onProgress(i + 1, total);
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
