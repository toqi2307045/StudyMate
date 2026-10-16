package com.example.studymate.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.studymate.data.entity.Course;
import com.example.studymate.util.TimeUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Tiny programmatic form builder so every add/edit dialog needs no XML. */
public class FormDialog {

    public static class DateField {
        public long millis;
        public Button button;
    }

    public static class TimeField {
        public int minutes;
        public Button button;
    }

    public static class ColorField {
        public int color;
    }

    public static class CourseField {
        public Spinner spinner;
        private List<Course> courses;

        /** 0 = none. */
        public long selectedId() {
            int p = spinner.getSelectedItemPosition();
            return p <= 0 || p > courses.size() ? 0 : courses.get(p - 1).id;
        }
    }

    private final Context ctx;
    private final LinearLayout root;

    public FormDialog(Context ctx) {
        this.ctx = ctx;
        root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = UiUtil.dp(ctx, 20);
        root.setPadding(p, UiUtil.dp(ctx, 8), p, 0);
    }

    private void add(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = UiUtil.dp(ctx, 8);
        root.addView(v, lp);
    }

    private TextView label(String s) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(12);
        t.setAlpha(0.7f);
        add(t);
        return t;
    }

    public EditText text(String hint, String initial) {
        TextInputLayout til = new TextInputLayout(ctx);
        til.setHint(hint);
        TextInputEditText et = new TextInputEditText(til.getContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setText(initial == null ? "" : initial);
        til.addView(et);
        add(til);
        return et;
    }

    public EditText multiline(String hint, String initial) {
        EditText et = text(hint, initial);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setMinLines(3);
        et.setGravity(Gravity.TOP);
        return et;
    }

    public EditText number(String hint, String initial) {
        EditText et = text(hint, initial);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return et;
    }

    public Spinner spinner(String label, List<String> items, int selected) {
        label(label);
        Spinner sp = new Spinner(ctx);
        ArrayAdapter<String> ad = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setSelection(Math.max(0, Math.min(selected, items.size() - 1)));
        add(sp);
        return sp;
    }

    /** Spinner of courses with a leading "(none)" entry when allowNone is true. */
    public CourseField courseSpinner(String label, List<Course> courses, long selectedId, boolean allowNone) {
        List<String> names = new ArrayList<>();
        names.add(allowNone ? "(none)" : "Choose course");
        int sel = 0;
        for (int i = 0; i < courses.size(); i++) {
            names.add(courses.get(i).label());
            if (courses.get(i).id == selectedId) sel = i + 1;
        }
        CourseField f = new CourseField();
        f.courses = courses;
        f.spinner = spinner(label, names, sel);
        return f;
    }

    public DateField dateTime(String label, long initial, boolean withTime, boolean optional) {
        label(label + (optional ? " (long-press to clear)" : ""));
        DateField f = new DateField();
        f.millis = initial;
        f.button = new Button(ctx);
        f.button.setAllCaps(false);
        Runnable refresh = () -> f.button.setText(f.millis == 0 ? "Not set"
                : (withTime ? TimeUtil.formatDateTime(f.millis) : TimeUtil.formatDate(f.millis)));
        refresh.run();
        f.button.setOnClickListener(v -> {
            LocalDateTime base = f.millis == 0 ? LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
                    : LocalDateTime.ofInstant(Instant.ofEpochMilli(f.millis), ZoneId.systemDefault());
            new DatePickerDialog(ctx, (dp, y, m, d) -> {
                if (!withTime) {
                    f.millis = LocalDateTime.of(y, m + 1, d, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    refresh.run();
                    return;
                }
                new TimePickerDialog(ctx, (tp, h, min) -> {
                    f.millis = LocalDateTime.of(y, m + 1, d, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    refresh.run();
                }, base.getHour(), base.getMinute(), false).show();
            }, base.getYear(), base.getMonthValue() - 1, base.getDayOfMonth()).show();
        });
        if (optional) {
            f.button.setOnLongClickListener(v -> {
                f.millis = 0;
                refresh.run();
                return true;
            });
        }
        add(f.button);
        return f;
    }

    public TimeField time(String label, int initialMinutes) {
        label(label);
        TimeField f = new TimeField();
        f.minutes = initialMinutes;
        f.button = new Button(ctx);
        f.button.setAllCaps(false);
        f.button.setText(TimeUtil.hm(f.minutes));
        f.button.setOnClickListener(v -> new TimePickerDialog(ctx, (tp, h, m) -> {
            f.minutes = h * 60 + m;
            f.button.setText(TimeUtil.hm(f.minutes));
        }, f.minutes / 60, f.minutes % 60, false).show());
        add(f.button);
        return f;
    }

    public ColorField color(int initial) {
        label("Color");
        ColorField f = new ColorField();
        f.color = initial == 0 ? UiUtil.COURSE_COLORS[0] : initial;
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        List<View> dots = new ArrayList<>();
        Runnable paint = () -> {
            for (int i = 0; i < dots.size(); i++) {
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(UiUtil.COURSE_COLORS[i]);
                boolean sel = UiUtil.COURSE_COLORS[i] == f.color;
                g.setStroke(UiUtil.dp(ctx, sel ? 3 : 1), sel ? Color.WHITE : 0x33000000);
                dots.get(i).setBackground(g);
                dots.get(i).setScaleX(sel ? 1.15f : 1f);
                dots.get(i).setScaleY(sel ? 1.15f : 1f);
            }
        };
        for (int i = 0; i < UiUtil.COURSE_COLORS.length; i++) {
            View dot = new View(ctx);
            int size = UiUtil.dp(ctx, 26);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(UiUtil.dp(ctx, 2), UiUtil.dp(ctx, 2), UiUtil.dp(ctx, 2), UiUtil.dp(ctx, 2));
            final int color = UiUtil.COURSE_COLORS[i];
            dot.setOnClickListener(v -> { f.color = color; paint.run(); });
            row.addView(dot, lp);
            dots.add(dot);
        }
        paint.run();
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(ctx);
        hs.addView(row);
        add(hs);
        return f;
    }

    /** onOk returns true to dismiss the dialog (false keeps it open, e.g. validation failed). */
    public void show(String title, String positive, BooleanSupplier onOk) {
        ScrollView sv = new ScrollView(ctx);
        sv.addView(root);
        AlertDialog d = new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton(positive, null)
                .setNegativeButton("Cancel", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> { if (onOk.getAsBoolean()) d.dismiss(); }));
        d.show();
    }

    public static String str(EditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
}
