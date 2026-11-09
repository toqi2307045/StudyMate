package com.example.studymate.docs;

import java.util.List;

public final class ExporterFactory {
    private ExporterFactory() {}

    public static PdfExporter<List<ImagePage>> images(ImageOptions options) { return new ImagePdfExporter(options); }
}
