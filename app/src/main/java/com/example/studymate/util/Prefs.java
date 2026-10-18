package com.example.studymate.util;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class Prefs {
    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("StudyMate", Context.MODE_PRIVATE);
    }

    /** -1 = follow system, 0 = light, 1 = dark. */
    public static int darkMode(Context c) { return sp(c).getInt("dark_mode", -1); }

    public static void setDarkMode(Context c, int v) { sp(c).edit().putInt("dark_mode", v).apply(); }

    public static void applyTheme(Context c) {
        int m = darkMode(c);
        AppCompatDelegate.setDefaultNightMode(m == 1 ? AppCompatDelegate.MODE_NIGHT_YES
                : m == 0 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static int getInt(Context c, String key, int def) { return sp(c).getInt(key, def); }

    public static void putInt(Context c, String key, int v) { sp(c).edit().putInt(key, v).apply(); }

    public static boolean getBool(Context c, String key, boolean def) { return sp(c).getBoolean(key, def); }

    public static void putBool(Context c, String key, boolean v) { sp(c).edit().putBoolean(key, v).apply(); }

    public static int classLeadMin(Context c) { return getInt(c, "class_lead_min", 10); }
    public static int assignLeadHours(Context c) { return getInt(c, "assign_lead_h", 12); }
    public static boolean notesLock(Context c) { return getBool(c, "notes_lock", false); }
    public static int focusMin(Context c) { return getInt(c, "focus_min", 25); }
    public static int shortBreakMin(Context c) { return getInt(c, "short_min", 5); }
    public static int longBreakMin(Context c) { return getInt(c, "long_min", 15); }
    public static boolean pdfNight(Context c) { return getBool(c, "pdf_night", false); }
    public static String getString(Context c, String key, String def) { return sp(c).getString(key, def); }

    public static void putString(Context c, String key, String v) { sp(c).edit().putString(key, v).apply(); }

    public static void remove(Context c, String key) { sp(c).edit().remove(key).apply(); }
}
