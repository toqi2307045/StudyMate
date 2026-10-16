package com.example.studymate.docs;

/** Factory that picks the right parser strategy for a file extension. */
public final class ParserFactory {
    private ParserFactory() {}

    public static DocumentParser<PptxModel> pptx() { return new PptxParser(); }

    public static DocumentParser<String> docx() { return new DocxParser(); }

    public static DocumentParser<?> forExtension(String ext) {
        switch (ext == null ? "" : ext.toLowerCase()) {
            case "pptx": return pptx();
            case "docx": return docx();
            default: throw new IllegalArgumentException("No parser for ." + ext);
        }
    }
}
