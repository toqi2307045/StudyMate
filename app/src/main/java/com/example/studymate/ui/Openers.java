package com.example.studymate.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.studymate.util.FileUtil;

/** Opens a file in the right in-app viewer based on its type. */
public final class Openers {
    public static final String MIME_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private Openers() {}

    public static void open(Context c, String uri, String name, String type) {
        Class<?> target;
        switch (type) {
            case "pptx": target = PptxViewerActivity.class; break;
            case "docx": target = DocxViewerActivity.class; break;
            default: target = PdfReaderActivity.class; break;
        }
        c.startActivity(new Intent(c, target).putExtra("uri", uri).putExtra("name", name));
    }

    public static void open(Context c, Uri uri) {
        String name = FileUtil.displayName(c, uri);
        String ext = FileUtil.ext(name);
        open(c, uri.toString(), name, ext.equals("pptx") || ext.equals("docx") ? ext : "pdf");
    }
}
