package com.example.studymate;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.studymate.util.NoticeWorker;
import com.example.studymate.util.NotificationHelper;
import com.example.studymate.util.Prefs;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        Prefs.applyTheme(this);
        NoticeWorker.schedule(this);
    }
}
