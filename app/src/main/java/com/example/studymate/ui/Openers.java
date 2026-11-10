package com.example.studymate.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.studymate.util.FileUtil;

/** Opens a file in the PDF reader. */
public final class Openers {
    public static final String MIME_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private Openers() {}

    public static void open(Context c, String uri, String name, String type) {
        c.startActivity(new Intent(c, PdfReaderActivity.class).putExtra("uri", uri).putExtra("name", name));
    }

    public static void open(Context c, Uri uri) {
        String name = FileUtil.displayName(c, uri);
        open(c, uri.toString(), name, "pdf");
    }
}
