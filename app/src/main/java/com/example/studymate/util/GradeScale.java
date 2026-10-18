package com.example.studymate.util;

import java.util.Locale;

/** One grading scale: letters, grade points and the lowest percentage that earns each letter. */
public final class GradeScale {
    public final String id, name, summary;
    public final String[] letters;
    public final double[] points;
    /** Lowest percentage for each grade, ordered from the best grade to the worst. */
    public final int[] minPercent;

    public GradeScale(String id, String name, String summary, String[] letters, double[] points, int[] minPercent) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.letters = letters;
        this.points = points;
        this.minPercent = minPercent;
    }

    /** Highest grade point (top of the chart). */
    public double max() { return points[0]; }

    /** -1 if the letter is not part of this scale. */
    public int indexOf(String letter) {
        for (int i = 0; i < letters.length; i++) if (letters[i].equals(letter)) return i;
        return -1;
    }

    public int indexForPercent(double pct) {
        for (int i = 0; i < minPercent.length; i++) if (pct >= minPercent[i]) return i;
        return letters.length - 1;
    }

    /** e.g. "A+  ·  80-100%  ·  4.00" */
    public String label(int i) {
        int hi = i == 0 ? 100 : minPercent[i - 1] - 1;
        return String.format(Locale.US, "%s  ·  %d-%d%%  ·  %.2f", letters[i], minPercent[i], hi, points[i]);
    }
}