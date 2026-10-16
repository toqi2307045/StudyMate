package com.example.studymate.docs;

import android.net.Uri;

/** One page of the image-to-PDF job: source image plus user rotation (0/90/180/270). */
public class ImagePage {
    public final Uri uri;
    public int rotation;

    public ImagePage(Uri uri) { this.uri = uri; }
}
