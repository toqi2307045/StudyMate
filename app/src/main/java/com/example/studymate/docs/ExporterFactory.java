package com.example.studymate.docs;

import java.util.List;

/** Factory for the PDF exporter strategies. */
public final class ExporterFactory {
    private ExporterFactory() {}

    public static PdfExporter<PptxModel> slides() { return new PptxPdfExporter(); }

    public static PdfExporter<List<ImagePage>> images(ImageOptions options) { return new ImagePdfExporter(options); }
}
