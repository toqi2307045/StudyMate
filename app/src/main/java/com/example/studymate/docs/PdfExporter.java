package com.example.studymate.docs;

import android.content.Context;

import java.io.File;
import java.io.IOException;

/** Strategy interface for writing a PDF from some source (slides, images...). */
public interface PdfExporter<S> {
    interface Progress {
        void onProgress(int done, int total);
    }

    File export(Context context, S source, File out, Progress progress) throws IOException;
}
