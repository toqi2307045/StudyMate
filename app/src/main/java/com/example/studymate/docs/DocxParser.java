package com.example.studymate.docs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Converts word/document.xml into simple HTML: headings, bold/italic/underline, bullet and
 * numbered lists, tables and embedded images. The same HTML feeds both the reader (WebView)
 * and the DOCX-to-PDF converter (Android print system).
 */
public class DocxParser implements DocumentParser<String> {

    private static class Para {
        final StringBuilder html = new StringBuilder();
        String style, align;
        int numId = 0, ilvl = 0;
        boolean hasImage;
    }

    private static class Numbering {
        final Map<Integer, Integer> numToAbs = new HashMap<>();
        final Map<String, String> fmt = new HashMap<>(); // "abs:lvl" -> numFmt
        final Map<Integer, int[]> counters = new HashMap<>();

        String format(int numId, int lvl) {
            Integer abs = numToAbs.get(numId);
            String f = abs == null ? null : fmt.get(abs + ":" + lvl);
            return f == null ? "bullet" : f;
        }
    }

    private static final Pattern HEADING = Pattern.compile("(?i)^heading\\s*([1-6])$");

    private static void skipElement(XmlPullParser p) throws Exception {
        int depth = 1;
        while (depth > 0) {
            int t = p.next();
            if (t == XmlPullParser.START_TAG) depth++;
            else if (t == XmlPullParser.END_TAG) depth--;
            else if (t == XmlPullParser.END_DOCUMENT) return;
        }
    }

    private static boolean off(String v) {
        return v != null && (v.equals("0") || v.equals("false") || v.equals("none"));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Numbering readNumbering(ZipFile zip) {
        Numbering n = new Numbering();
        ZipEntry e = zip.getEntry("word/numbering.xml");
        if (e == null) return n;
        try (InputStream in = zip.getInputStream(e)) {
            XmlPullParser p = Xml.newPullParser();
            p.setInput(in, null);
            String abs = null, lvl = null, num = null;
            for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
                if (ev == XmlPullParser.START_TAG) {
                    switch (p.getName()) {
                        case "w:abstractNum": abs = p.getAttributeValue(null, "w:abstractNumId"); break;
                        case "w:lvl": lvl = p.getAttributeValue(null, "w:ilvl"); break;
                        case "w:numFmt":
                            if (abs != null && lvl != null) n.fmt.put(abs + ":" + lvl, p.getAttributeValue(null, "w:val"));
                            break;
                        case "w:num": num = p.getAttributeValue(null, "w:numId"); break;
                        case "w:abstractNumId":
                            if (num != null) n.numToAbs.put(Integer.parseInt(num), Integer.parseInt(p.getAttributeValue(null, "w:val")));
                            break;
                        default: break;
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    if (p.getName().equals("w:abstractNum")) { abs = null; lvl = null; }
                    else if (p.getName().equals("w:num")) num = null;
                }
            }
        } catch (Exception ignored) { }
        return n;
    }

    private static String roman(int n) {
        int[] v = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] s = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) while (n >= v[i]) { n -= v[i]; sb.append(s[i]); }
        return sb.toString();
    }

    private static String label(String fmt, int lvl, int n) {
        switch (fmt) {
            case "bullet": return lvl % 2 == 0 ? "•" : "◦";
            case "lowerLetter": return (char) ('a' + (n - 1) % 26) + ".";
            case "upperLetter": return (char) ('A' + (n - 1) % 26) + ".";
            case "lowerRoman": return roman(n).toLowerCase() + ".";
            case "upperRoman": return roman(n) + ".";
            default: return n + ".";
        }
    }

    private static String imageTag(ZipFile zip, String path, long cx) {
        try {
            ZipEntry e = zip.getEntry(path);
            if (e == null) return "<i>[image]</i>";
            byte[] data;
            try (InputStream in = zip.getInputStream(e)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                data = bos.toByteArray();
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= 1200) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (bmp == null) return "<i>[unsupported image]</i>";
            boolean png = path.toLowerCase().endsWith(".png");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 85, out);
            bmp.recycle();
            String b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            String width = cx > 0 ? " width=\"" + Math.min(900, cx / 9525) + "\"" : "";
            return "<img" + width + " src=\"data:image/" + (png ? "png" : "jpeg") + ";base64," + b64 + "\">";
        } catch (Exception ex) {
            return "<i>[image]</i>";
        }
    }

    @Override
    public String parse(File file) throws Exception {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry docEntry = zip.getEntry("word/document.xml");
            if (docEntry == null) throw new IOException("Not a valid .docx file");

            Map<String, String> rels = new HashMap<>();
            for (PptxParser.Rel r : PptxParser.readRels(zip, "word/document.xml")) rels.put(r.id, r.target);
            Numbering numbering = readNumbering(zip);

            StringBuilder out = new StringBuilder();
            Deque<Para> stack = new ArrayDeque<>();
            int tableDepth = 0;
            boolean inPPr = false, inRun = false, inRPr = false;
            boolean bold = false, italic = false, underline = false, strike = false;
            long extCx = 0;

            try (InputStream in = zip.getInputStream(docEntry)) {
                XmlPullParser p = Xml.newPullParser();
                p.setInput(in, null);
                int ev = p.getEventType();
                while (ev != XmlPullParser.END_DOCUMENT) {
                    if (ev == XmlPullParser.START_TAG) {
                        String n = p.getName();
                        Para cur = stack.peek();
                        switch (n) {
                            case "mc:Fallback": skipElement(p); break;
                            case "w:p": stack.push(new Para()); inPPr = false; break;
                            case "w:pPr": inPPr = true; break;
                            case "w:pStyle": if (cur != null && inPPr) cur.style = p.getAttributeValue(null, "w:val"); break;
                            case "w:numId":
                                if (cur != null && inPPr) cur.numId = parseInt(p.getAttributeValue(null, "w:val"));
                                break;
                            case "w:ilvl":
                                if (cur != null && inPPr) cur.ilvl = parseInt(p.getAttributeValue(null, "w:val"));
                                break;
                            case "w:jc": if (cur != null && inPPr) cur.align = p.getAttributeValue(null, "w:val"); break;
                            case "w:r": inRun = true; inRPr = false; bold = italic = underline = strike = false; break;
                            case "w:rPr": inRPr = true; break;
                            case "w:b": if (inRun && inRPr) bold = !off(p.getAttributeValue(null, "w:val")); break;
                            case "w:i": if (inRun && inRPr) italic = !off(p.getAttributeValue(null, "w:val")); break;
                            case "w:u": if (inRun && inRPr) underline = !off(p.getAttributeValue(null, "w:val")); break;
                            case "w:strike": if (inRun && inRPr) strike = !off(p.getAttributeValue(null, "w:val")); break;
                            case "w:t": {
                                String text = p.nextText();
                                if (cur != null && !text.isEmpty()) {
                                    String h = escape(text);
                                    if (bold) h = "<b>" + h + "</b>";
                                    if (italic) h = "<i>" + h + "</i>";
                                    if (underline) h = "<u>" + h + "</u>";
                                    if (strike) h = "<s>" + h + "</s>";
                                    cur.html.append(h);
                                }
                                break;
                            }
                            case "w:tab": if (cur != null) cur.html.append("&emsp;"); break;
                            case "w:br":
                                if (cur != null && !"page".equals(p.getAttributeValue(null, "w:type"))) cur.html.append("<br>");
                                break;
                            case "wp:extent": extCx = parseLong(p.getAttributeValue(null, "cx")); break;
                            case "a:blip": {
                                String target = rels.get(p.getAttributeValue(null, "r:embed"));
                                if (cur != null && target != null) { cur.html.append(imageTag(zip, target, extCx)); cur.hasImage = true; }
                                break;
                            }
                            case "v:imagedata": {
                                String target = rels.get(p.getAttributeValue(null, "r:id"));
                                if (cur != null && target != null) { cur.html.append(imageTag(zip, target, 0)); cur.hasImage = true; }
                                break;
                            }
                            case "w:tbl": tableDepth++; out.append("<table>"); break;
                            case "w:tr": out.append("<tr>"); break;
                            case "w:tc": out.append("<td>"); break;
                            default: break;
                        }
                    } else if (ev == XmlPullParser.END_TAG) {
                        switch (p.getName()) {
                            case "w:pPr": inPPr = false; break;
                            case "w:rPr": inRPr = false; break;
                            case "w:r": inRun = false; break;
                            case "w:p": {
                                Para pa = stack.poll();
                                if (pa != null) emit(out, pa, numbering, tableDepth > 0);
                                break;
                            }
                            case "w:tc": out.append("</td>"); break;
                            case "w:tr": out.append("</tr>"); break;
                            case "w:tbl": tableDepth = Math.max(0, tableDepth - 1); out.append("</table>"); break;
                            default: break;
                        }
                    }
                    ev = p.next();
                }
            }
            return out.toString();
        }
    }

    private static int parseInt(String s) {
        try { return s == null ? 0 : Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static long parseLong(String s) {
        try { return s == null ? 0 : Long.parseLong(s); } catch (NumberFormatException e) { return 0; }
    }

    private static void emit(StringBuilder out, Para pa, Numbering numbering, boolean inTable) {
        String inner = pa.html.toString();
        if (inner.isEmpty() && !pa.hasImage) {
            if (!inTable) out.append("<p>&nbsp;</p>");
            return;
        }
        String align = "";
        if (pa.align != null) {
            switch (pa.align) {
                case "center": align = "text-align:center;"; break;
                case "right": case "end": align = "text-align:right;"; break;
                case "both": align = "text-align:justify;"; break;
                default: break;
            }
        }
        int heading = 0;
        if (pa.style != null) {
            Matcher m = HEADING.matcher(pa.style);
            if (m.matches()) heading = Integer.parseInt(m.group(1));
            else if (pa.style.equalsIgnoreCase("Title")) heading = 1;
        }
        if (heading > 0) {
            out.append("<h").append(heading).append(" style=\"").append(align).append("\">")
                    .append(inner).append("</h").append(heading).append(">");
        } else if (pa.numId > 0) {
            int[] counters = numbering.counters.computeIfAbsent(pa.numId, k -> new int[10]);
            int lvl = Math.max(0, Math.min(9, pa.ilvl));
            counters[lvl]++;
            for (int i = lvl + 1; i < 10; i++) counters[i] = 0;
            String lab = label(numbering.format(pa.numId, lvl), lvl, counters[lvl]);
            out.append("<p style=\"margin-left:").append(22 * (lvl + 1)).append("px;text-indent:-18px;")
                    .append(align).append("\">").append(lab).append("&nbsp;&nbsp;").append(inner).append("</p>");
        } else {
            out.append("<p style=\"").append(align).append("\">").append(inner).append("</p>");
        }
    }

    public static String wrap(String body, boolean dark) {
        String bg = dark ? "#121212" : "#ffffff", fg = dark ? "#e6e6e6" : "#111111";
        String border = dark ? "#777" : "#888";
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<style>body{font-family:sans-serif;line-height:1.45;padding:12px;margin:0;"
                + "background:" + bg + ";color:" + fg + ";word-wrap:break-word}"
                + "p{margin:0 0 8px 0;white-space:pre-wrap}"
                + "h1,h2,h3,h4,h5,h6{margin:14px 0 8px 0;line-height:1.25}"
                + "table{border-collapse:collapse;margin:8px 0;max-width:100%}"
                + "td{border:1px solid " + border + ";padding:4px 6px;vertical-align:top}"
                + "img{max-width:100%;height:auto}</style></head><body>" + body + "</body></html>";
    }
}
