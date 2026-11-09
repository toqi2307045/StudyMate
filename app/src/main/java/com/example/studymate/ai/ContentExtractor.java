package com.example.studymate.ai;

import android.content.Context;
import android.net.Uri;

import com.example.studymate.docs.ParserFactory;
import com.example.studymate.docs.PptxModel;
import com.example.studymate.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ContentExtractor {

    public static class ExtractedContent {
        public final String fileName;
        public final String mimeType;
        public final byte[] bytes;
        public final String text;

        public ExtractedContent(String fileName, String mimeType, byte[] bytes, String text) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.bytes = bytes;
            this.text = text;
        }

        public boolean isMedia() {
            return bytes != null && bytes.length > 0;
        }
    }

    public static ExtractedContent extract(Context context, Uri uri, String name) throws Exception {
        if (uri == null) throw new IllegalArgumentException("URI cannot be null");
        String displayName = name == null ? FileUtil.displayName(context, uri) : name;
        String ext = FileUtil.ext(displayName).toLowerCase();

        File cachedFile = FileUtil.copyToCache(context, uri, ext);
        if (cachedFile.length() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 20MB limit.");
        }

        if ("pdf".equals(ext)) {
            byte[] bytes = readFileBytes(cachedFile);
            return new ExtractedContent(displayName, "application/pdf", bytes, null);
        } else if ("pptx".equals(ext)) {
            PptxModel model = ParserFactory.pptx().parse(cachedFile);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < model.slides.size(); i++) {
                PptxModel.Slide slide = model.slides.get(i);
                sb.append("[Slide ").append(i + 1).append("]\n");
                sb.append(slide.plainText()).append("\n\n");
            }
            model.close();
            return new ExtractedContent(displayName, "text/plain", null, sb.toString());
        } else if ("docx".equals(ext)) {
            String html = ParserFactory.docx().parse(cachedFile);
            String plainText = stripHtml(html);
            return new ExtractedContent(displayName, "text/plain", null, plainText);
        } else if (ext.matches("png|jpg|jpeg|webp")) {
            byte[] bytes = readFileBytes(cachedFile);
            String mime = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
            return new ExtractedContent(displayName, mime, bytes, null);
        } else {
            throw new IllegalArgumentException("Unsupported file type: ." + ext);
        }
    }

    private static byte[] readFileBytes(File file) throws Exception {
        try (InputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}
