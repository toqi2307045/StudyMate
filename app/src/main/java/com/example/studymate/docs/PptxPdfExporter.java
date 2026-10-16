package com.example.studymate.docs;

import android.content.Context;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Draws every slide onto its own PDF page (page size = slide size in points). */
public class PptxPdfExporter implements PdfExporter<PptxModel> {

    @Override
    public File export(Context context, PptxModel model, File out, Progress progress) throws IOException {
        int pw = Math.max(1, Math.round(model.widthEmu / 12700f));
        int ph = Math.max(1, Math.round(model.heightEmu / 12700f));
        PdfDocument doc = new PdfDocument();
        try {
            int total = model.slides.size();
            for (int i = 0; i < total; i++) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pw, ph, i + 1).create();
                PdfDocument.Page page = doc.startPage(info);
                SlideRenderer.draw(model, model.slides.get(i), page.getCanvas(), pw, ph, 1600);
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
