package com.example.studymate.docs;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

/** Colour filters shared by the live preview and the PDF export ("document scan" mode). */
public final class ImageFilters {
    private ImageFilters() {}

    private static ColorMatrix contrast(float c, float brightness) {
        float t = (-0.5f * c + 0.5f) * 255f + brightness;
        return new ColorMatrix(new float[]{
                c, 0, 0, 0, t,
                0, c, 0, 0, t,
                0, 0, c, 0, t,
                0, 0, 0, 1, 0});
    }

    /** index matches ImageOptions.FILTERS; returns null for "Original". */
    public static ColorMatrixColorFilter filter(int index) {
        switch (index) {
            case 1: {
                ColorMatrix m = new ColorMatrix();
                m.setSaturation(0);
                return new ColorMatrixColorFilter(m);
            }
            case 2:
                return new ColorMatrixColorFilter(contrast(1.35f, 8f));
            case 3: {
                ColorMatrix m = new ColorMatrix();
                m.setSaturation(0);
                m.postConcat(contrast(2.4f, 35f));
                return new ColorMatrixColorFilter(m);
            }
            default:
                return null;
        }
    }
}
