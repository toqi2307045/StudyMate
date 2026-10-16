package com.example.studymate.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** Strategy interface: one implementation per export/import file format. */
public interface DataFormat {
    String extension();

    String mimeType();

    void write(Map<String, List<Map<String, Object>>> data, OutputStream out) throws IOException;

    Map<String, List<Map<String, Object>>> read(InputStream in) throws IOException;
}
