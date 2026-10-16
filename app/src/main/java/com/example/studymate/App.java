package com.example.studymate;

import android.app.Application;

import com.example.studymate.util.NotificationHelper;
import com.example.studymate.util.Prefs;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        Prefs.applyTheme(this);
    }
}
