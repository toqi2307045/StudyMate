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
    public static int longBreakEvery(Context c) { return getInt(c, "long_every", 4); }
    public static boolean pdfNight(Context c) { return getBool(c, "pdf_night", false); }
    public static String getString(Context c, String key, String def) { return sp(c).getString(key, def); }

    public static void putString(Context c, String key, String v) { sp(c).edit().putString(key, v).apply(); }

    public static String openRouterKey(Context c) { return getString(c, "openrouter_key", ""); }

    public static void setOpenRouterKey(Context c, String key) { putString(c, "openrouter_key", key); }

    public static String nickname(Context c) {
        String name = getString(c, "user_nickname", "");
        if (name == null || name.trim().isEmpty()) {
            com.google.firebase.auth.FirebaseUser u = com.example.studymate.firebase.FirebaseSync.currentUser();
            if (u != null) {
                if (u.getDisplayName() != null && !u.getDisplayName().trim().isEmpty()) {
                    return u.getDisplayName();
                } else if (u.getEmail() != null && u.getEmail().contains("@")) {
                    return u.getEmail().substring(0, u.getEmail().indexOf('@'));
                }
            }
            return "Student";
        }
        return name;
    }

    public static void setNickname(Context c, String name) { putString(c, "user_nickname", name); }

    public static String selectedAiModelId(Context c) { return getString(c, "ai_model_id", "gemini_flash"); }

    public static void setSelectedAiModelId(Context c, String id) { putString(c, "ai_model_id", id); }

    public static boolean aiEnabled(Context c) { return getBool(c, "ai_enabled", true); }

    public static void setAiEnabled(Context c, boolean v) { putBool(c, "ai_enabled", v); }

    public static boolean aiConsent(Context c) { return getBool(c, "ai_consent", false); }

    public static void setAiConsent(Context c, boolean v) { putBool(c, "ai_consent", v); }

    public static void remove(Context c, String key) { sp(c).edit().remove(key).apply(); }
}
