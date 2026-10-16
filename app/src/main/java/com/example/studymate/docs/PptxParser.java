package com.example.studymate.docs;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Parses the OOXML inside a .pptx (zip + XML) using XmlPullParser - no external library. */
public class PptxParser implements DocumentParser<PptxModel> {

    static class Rel {
        String id, type, target;
    }

    static String resolve(String base, String target) {
        if (target.startsWith("/")) return target.substring(1);
        List<String> parts = new ArrayList<>();
        String[] b = base.split("/");
        for (int i = 0; i < b.length - 1; i++) parts.add(b[i]);
        for (String t : target.split("/")) {
            if (t.equals("..")) {
                if (!parts.isEmpty()) parts.remove(parts.size() - 1);
            } else if (!t.equals(".") && !t.isEmpty()) {
                parts.add(t);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) sb.append(i > 0 ? "/" : "").append(parts.get(i));
        return sb.toString();
    }

    static String relsPathFor(String part) {
        int i = part.lastIndexOf('/');
        return part.substring(0, i + 1) + "_rels/" + part.substring(i + 1) + ".rels";
    }

    static List<Rel> readRels(ZipFile zip, String part) {
        List<Rel> rels = new ArrayList<>();
        ZipEntry e = zip.getEntry(relsPathFor(part));
        if (e == null) return rels;
        try (InputStream in = zip.getInputStream(e)) {
            XmlPullParser p = Xml.newPullParser();
            p.setInput(in, null);
            for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
                if (ev == XmlPullParser.START_TAG && p.getName().equals("Relationship")) {
                    Rel r = new Rel();
                    r.id = p.getAttributeValue(null, "Id");
                    r.type = p.getAttributeValue(null, "Type");
                    String t = p.getAttributeValue(null, "Target");
                    r.target = t == null ? null : resolve(part, t);
                    rels.add(r);
                }
            }
        } catch (Exception ignored) { }
        return rels;
    }

    private static long num(String s) {
        try {
            return s == null ? -1 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static List<PptxModel.Shape> parseShapes(InputStream in) throws Exception {
        List<PptxModel.Shape> out = new ArrayList<>();
        XmlPullParser p = Xml.newPullParser();
        p.setInput(in, null);
        PptxModel.Shape cur = null;
        PptxModel.Para para = null;
        for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
            if (ev == XmlPullParser.START_TAG) {
                String n = p.getName();
                switch (n) {
                    case "p:sp":
                    case "p:pic":
                    case "p:graphicFrame":
                        if (cur == null) {
                            cur = new PptxModel.Shape();
                            cur.image = n.equals("p:pic");
                        }
                        break;
                    case "p:ph":
                        if (cur != null) {
                            String t = p.getAttributeValue(null, "type");
                            cur.placeholder = t == null ? "body" : t;
                        }
                        break;
                    case "a:off":
                        if (cur != null && cur.x < 0) {
                            cur.x = num(p.getAttributeValue(null, "x"));
                            cur.y = num(p.getAttributeValue(null, "y"));
                        }
                        break;
                    case "a:ext":
                        if (cur != null && cur.w < 0 && p.getAttributeValue(null, "cx") != null) {
                            cur.w = num(p.getAttributeValue(null, "cx"));
                            cur.h = num(p.getAttributeValue(null, "cy"));
                        }
                        break;
                    case "a:blip":
                        if (cur != null) cur.embedId = p.getAttributeValue(null, "r:embed");
                        break;
                    case "a:p":
                        if (cur != null) para = new PptxModel.Para();
                        break;
                    case "a:buChar":
                    case "a:buAutoNum":
                        if (para != null) para.bullet = true;
                        break;
                    case "a:buNone":
                        if (para != null) para.noBullet = true;
                        break;
                    case "a:rPr":
                        if (para != null) {
                            String sz = p.getAttributeValue(null, "sz");
                            if (sz != null && para.sizePt == 0) para.sizePt = num(sz) / 100f;
                            if ("1".equals(p.getAttributeValue(null, "b"))) para.bold = true;
                        }
                        break;
                    case "a:t":
                        if (para != null) para.text += p.nextText();
                        break;
                    case "a:br":
                        if (para != null) para.text += "\n";
                        break;
                    default:
                        break;
                }
            } else if (ev == XmlPullParser.END_TAG) {
                String n = p.getName();
                if (n.equals("a:p")) {
                    if (cur != null && para != null) {
                        if (!para.text.trim().isEmpty()) cur.paras.add(para);
                        para = null;
                    }
                } else if (n.equals("p:sp") || n.equals("p:pic") || n.equals("p:graphicFrame")) {
                    if (cur != null) {
                        String ph = cur.placeholder;
                        boolean junk = "sldNum".equals(ph) || "dt".equals(ph) || "ftr".equals(ph);
                        if (!junk && (cur.image || !cur.paras.isEmpty())) {
                            boolean body = "body".equals(ph) || "subTitle".equals(ph);
                            for (PptxModel.Para pp : cur.paras) {
                                if (body && !pp.noBullet && !"subTitle".equals(ph)) pp.bullet = true;
                            }
                            out.add(cur);
                        }
                        cur = null;
                    }
                }
            }
        }
        return out;
    }

    @Override
    public PptxModel parse(java.io.File file) throws Exception {
        ZipFile zip = new ZipFile(file);
        PptxModel model = new PptxModel();
        model.zip = zip;
        try {
            String presPath = "ppt/presentation.xml";
            ZipEntry pe = zip.getEntry(presPath);
            if (pe == null) throw new IOException("Not a valid .pptx file");

            Map<String, String> relTargets = new HashMap<>();
            for (Rel r : readRels(zip, presPath)) relTargets.put(r.id, r.target);

            List<String> slidePaths = new ArrayList<>();
            try (InputStream in = zip.getInputStream(pe)) {
                XmlPullParser p = Xml.newPullParser();
                p.setInput(in, null);
                for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
                    if (ev != XmlPullParser.START_TAG) continue;
                    if (p.getName().equals("p:sldSz")) {
                        long cx = num(p.getAttributeValue(null, "cx"));
                        long cy = num(p.getAttributeValue(null, "cy"));
                        if (cx > 0 && cy > 0) { model.widthEmu = cx; model.heightEmu = cy; }
                    } else if (p.getName().equals("p:sldId")) {
                        String t = relTargets.get(p.getAttributeValue(null, "r:id"));
                        if (t != null) slidePaths.add(t);
                    }
                }
            }

            for (String sp : slidePaths) {
                PptxModel.Slide slide = new PptxModel.Slide();
                ZipEntry se = zip.getEntry(sp);
                if (se == null) continue;
                try (InputStream in = zip.getInputStream(se)) {
                    slide.shapes.addAll(parseShapes(in));
                }
                String notesPath = null;
                Map<String, String> media = new HashMap<>();
                for (Rel r : readRels(zip, sp)) {
                    media.put(r.id, r.target);
                    if (r.type != null && r.type.endsWith("/notesSlide")) notesPath = r.target;
                }
                for (PptxModel.Shape s : slide.shapes) {
                    if (s.embedId != null) s.mediaPath = media.get(s.embedId);
                }
                if (notesPath != null && zip.getEntry(notesPath) != null) {
                    try (InputStream in = zip.getInputStream(zip.getEntry(notesPath))) {
                        StringBuilder nb = new StringBuilder();
                        for (PptxModel.Shape s : parseShapes(in)) {
                            if (!"body".equals(s.placeholder)) continue;
                            for (PptxModel.Para pp : s.paras) {
                                if (nb.length() > 0) nb.append('\n');
                                nb.append(pp.text);
                            }
                        }
                        slide.notes = nb.toString();
                    } catch (Exception ignored) { }
                }
                model.slides.add(slide);
            }
            if (model.slides.isEmpty()) throw new IOException("No slides found in this file");
            return model;
        } catch (Exception e) {
            model.close();
            throw e;
        }
    }
}
