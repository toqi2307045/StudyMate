package com.example.studymate.ui;

import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public abstract class BaseActivity extends AppCompatActivity {

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
