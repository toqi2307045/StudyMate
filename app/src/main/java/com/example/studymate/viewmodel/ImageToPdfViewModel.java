package com.example.studymate.viewmodel;

import android.net.Uri;

import androidx.lifecycle.ViewModel;

import com.example.studymate.docs.ImageOptions;
import com.example.studymate.docs.ImagePage;

import java.util.ArrayList;
import java.util.List;

/** Keeps the page list and export options alive across rotation. */
public class ImageToPdfViewModel extends ViewModel {
    public final List<ImagePage> pages = new ArrayList<>();
    public final ImageOptions options = new ImageOptions();
    public Uri cameraUri;
}
