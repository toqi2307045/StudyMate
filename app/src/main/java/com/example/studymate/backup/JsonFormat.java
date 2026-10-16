package com.example.studymate.backup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonFormat implements DataFormat {
    @Override public String extension() { return "json"; }

    @Override public String mimeType() { return "application/json"; }

    @Override
    public void write(Map<String, List<Map<String, Object>>> data, OutputStream out) throws IOException {
        try {
            JSONObject root = new JSONObject();
            root.put("app", "StudyMate");
            root.put("version", 1);
            JSONObject tables = new JSONObject();
            for (Map.Entry<String, List<Map<String, Object>>> t : data.entrySet()) {
                JSONArray arr = new JSONArray();
                for (Map<String, Object> row : t.getValue()) {
                    JSONObject o = new JSONObject();
                    for (Map.Entry<String, Object> e : row.entrySet())
                        o.put(e.getKey(), e.getValue() == null ? JSONObject.NULL : e.getValue());
                    arr.put(o);
                }
                tables.put(t.getKey(), arr);
            }
            root.put("tables", tables);
            out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Map<String, List<Map<String, Object>>> read(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        try {
            JSONObject root = new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
            JSONObject tables = root.getJSONObject("tables");
            Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
            Iterator<String> keys = tables.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                JSONArray arr = tables.getJSONArray(name);
                List<Map<String, Object>> rows = new java.util.ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Map<String, Object> row = new LinkedHashMap<>();
                    Iterator<String> fk = o.keys();
                    while (fk.hasNext()) {
                        String k = fk.next();
                        row.put(k, o.isNull(k) ? null : o.get(k));
                    }
                    rows.add(row);
                }
                result.put(name, rows);
            }
            return result;
        } catch (JSONException e) {
            throw new IOException("Not a valid StudyMate JSON backup", e);
        }
    }
}
