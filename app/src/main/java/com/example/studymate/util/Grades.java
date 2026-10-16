package com.example.studymate.util;
public final class Grades {
    private Grades() {}

    /** 4.0 scale with plus/minus grades. Edit these two arrays to match your university. */
    public static final String[] LETTERS = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "D", "F"};
    public static final double[] POINTS = {4.00, 3.75, 3.50, 3.25, 3.00, 2.75, 2.50, 2.25, 2.00, 0.00};

    public static double point(String letter) {
        for (int i = 0; i < LETTERS.length; i++) if (LETTERS[i].equals(letter)) return POINTS[i];
        return 0;
    }

    public static int indexOf(String letter) {
        for (int i = 0; i < LETTERS.length; i++) if (LETTERS[i].equals(letter)) return i;
        return 0;
    }
}
