package com.example.studymate.docs;

/** Export options for the image-to-PDF converter. */
public class ImageOptions {
    public static final String[] PAGE_SIZES = {"A4", "Letter", "Fit to image"};
    public static final String[] QUALITIES = {"Low (small file)", "Medium", "High (sharpest)"};
    public static final String[] FILTERS = {"Original", "Grayscale", "Contrast boost", "Scan (black & white)"};
    private static final int[] MAX_DIM = {1000, 1600, 2400};

    public int pageSize = 0;
    public int quality = 1;
    public int filter = 0;

    public int maxDim() { return MAX_DIM[quality]; }

    public String summary() {
        return PAGE_SIZES[pageSize] + " · " + QUALITIES[quality] + " · " + FILTERS[filter];
    }
}
