package com.example.studymate.backup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sectioned CSV: a "#table,name" row starts each table, followed by a header row and data
 * rows; a blank line ends the table. Fields with commas, quotes or newlines are quoted.
 */
public class CsvFormat implements DataFormat {
    @Override public String extension() { return "csv"; }

    @Override public String mimeType() { return "text/csv"; }

    private static String esc(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    @Override
    public void write(Map<String, List<Map<String, Object>>> data, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Map<String, Object>>> t : data.entrySet()) {
            sb.append("#table,").append(t.getKey()).append('\n');
            List<Map<String, Object>> rows = t.getValue();
            if (rows.isEmpty()) {
                sb.append("id\n\n");
                continue;
            }
            List<String> cols = new ArrayList<>(rows.get(0).keySet());
            for (int i = 0; i < cols.size(); i++) sb.append(i > 0 ? "," : "").append(cols.get(i));
            sb.append('\n');
            for (Map<String, Object> row : rows) {
                for (int i = 0; i < cols.size(); i++) sb.append(i > 0 ? "," : "").append(esc(row.get(cols.get(i))));
                sb.append('\n');
            }
            sb.append('\n');
        }
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Minimal RFC-4180 parser (handles quoted newlines and doubled quotes). */
    static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else quoted = false;
                } else cell.append(ch);
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                row.add(cell.toString()); cell.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString()); cell.setLength(0);
                rows.add(row); row = new ArrayList<>();
            } else cell.append(ch);
        }
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }

    @Override
    public Map<String, List<Map<String, Object>>> read(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        String text = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        List<List<String>> rows = parse(text);
        String table = null;
        List<String> header = null;
        for (List<String> r : rows) {
            boolean blank = r.size() == 1 && r.get(0).isEmpty();
            if (!r.isEmpty() && r.get(0).equals("#table") && r.size() >= 2) {
                table = r.get(1);
                result.put(table, new ArrayList<>());
                header = null;
            } else if (table == null || blank) {
                if (blank) { table = null; header = null; }
            } else if (header == null) {
                header = r;
            } else {
                Map<String, Object> m = new LinkedHashMap<>();
                for (int i = 0; i < header.size() && i < r.size(); i++) m.put(header.get(i), r.get(i));
                result.get(table).add(m);
            }
        }
        if (result.isEmpty()) throw new IOException("Not a valid StudyMate CSV backup");
        return result;
    }
}
