package com.example.studymate.docs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Draws every slide straight onto a PDF page (vector text, no per-slide bitmap). */
public class PptxPdfExporter implements PdfExporter<PptxModel> {

    @Override
    public File export(Context context, PptxModel model, File out, Progress progress) throws IOException {
        if (model == null || model.slides.isEmpty()) {
            throw new IOException("Presentation contains no slides");
        }

        // PDF points (1/72 inch)
        int pdfW = Math.max(1, Math.round(model.widthEmu / 12700f));
        int pdfH = Math.max(1, Math.round(model.heightEmu / 12700f));

        PdfDocument doc = new PdfDocument();
        // Images must stay alive until the document is written to disk.
        List<Bitmap> keepAlive = new ArrayList<>();
        try {
            int total = model.slides.size();
            for (int i = 0; i < total; i++) {
                PdfDocument.Page page = doc.startPage(
                        new PdfDocument.PageInfo.Builder(pdfW, pdfH, i + 1).create());
                SlideRenderer.draw(model, model.slides.get(i), page.getCanvas(),
                        pdfW, pdfH, 1280, keepAlive);
                doc.finishPage(page);
                if (progress != null) progress.onProgress(i + 1, total);
            }
            try (FileOutputStream os = new FileOutputStream(out)) {
                doc.writeTo(os);
            }
        } finally {
            doc.close();
            for (Bitmap b : keepAlive) b.recycle();
        }
        return out;
    }
}