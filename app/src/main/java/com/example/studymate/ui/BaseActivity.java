package com.example.studymate.ui;

import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studymate.receiver.DarkThemeReceiver;
import com.google.android.material.appbar.MaterialToolbar;

public abstract class BaseActivity extends AppCompatActivity {

    private DarkThemeReceiver darkThemeReceiver;

    @Override
    protected void onResume() {
        super.onResume();
        if (darkThemeReceiver == null) {
            darkThemeReceiver = new DarkThemeReceiver();
        }
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
            registerReceiver(darkThemeReceiver, filter);
        } catch (Exception ignored) { }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (darkThemeReceiver != null) {
            try {
                unregisterReceiver(darkThemeReceiver);
            } catch (Exception ignored) { }
        }
    }

    protected void setupToolbar(MaterialToolbar toolbar, String title, boolean back) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(back);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    /** Adds an overflow menu item (no menu XML needed). */
    protected MenuItem addMenu(Menu menu, int id, String title) {
        MenuItem item = menu.add(Menu.NONE, id, Menu.NONE, title);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return item;
    }
}
