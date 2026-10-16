package com.example.studymate.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.IntConsumer;

public final class UiUtil {
    private UiUtil() {}

    public static final int[] COURSE_COLORS = {
            0xFF3F51B5, 0xFFE91E63, 0xFF009688, 0xFFFF9800, 0xFF9C27B0,
            0xFF4CAF50, 0xFFF44336, 0xFF00BCD4, 0xFF795548, 0xFF607D8B
    };

    public static int dp(Context c, int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static void toast(Context c, String msg) { Toast.makeText(c, msg, Toast.LENGTH_SHORT).show(); }

    public static boolean isNight(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void confirm(Context c, String title, String message, String positive, Runnable onYes) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(title).setMessage(message)
                .setPositiveButton(positive, (d, w) -> onYes.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Single-select segmented control (Material button toggle group). */
    public static MaterialButtonToggleGroup segmented(Context c, String[] labels, int selected, IntConsumer onSelect) {
        MaterialButtonToggleGroup group = new MaterialButtonToggleGroup(c);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        int[] ids = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            MaterialButton b = new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            ids[i] = View.generateViewId();
            b.setId(ids[i]);
            b.setText(labels[i]);
            group.addView(b);
        }
        group.check(ids[selected]);
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < ids.length; i++) if (ids[i] == checkedId) onSelect.accept(i);
        });
        return group;
    }
}
