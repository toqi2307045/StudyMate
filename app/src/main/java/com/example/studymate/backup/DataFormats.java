package com.example.studymate.backup;

/** Factory that returns the right {@link DataFormat} strategy. */
public final class DataFormats {
    private DataFormats() {}

    public static DataFormat json() { return new JsonFormat(); }

    public static DataFormat csv() { return new CsvFormat(); }

    public static DataFormat forName(String fileName) {
        String n = fileName == null ? "" : fileName.toLowerCase();
        return n.endsWith(".csv") ? csv() : json();
    }
}
