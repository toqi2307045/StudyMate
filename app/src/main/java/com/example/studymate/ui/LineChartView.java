package com.example.studymate.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/** Minimal multi-series line chart (no external chart library). */
public class LineChartView extends View {
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<String> labels = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private List<List<Double>> series = new ArrayList<>();
    private int[] colors = {0xFF3F51B5, 0xFFFF9800};
    private double yMax = 4.0;

    public LineChartView(Context c, AttributeSet a) {
        super(c, a);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(UiUtil.dp(c, 3));
        line.setStrokeJoin(Paint.Join.ROUND);
        grid.setStrokeWidth(1f);
        text.setTextSize(UiUtil.dp(c, 11));
        axis.setStrokeWidth(2f);
    }

    public void setData(List<String> labels, List<String> names, List<List<Double>> series, double yMax) {
        this.labels = labels;
        this.names = names;
        this.series = series;
        this.yMax = yMax;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        int onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        axis.setColor(onSurface);
        grid.setColor((onSurface & 0x00FFFFFF) | 0x22000000);
        text.setColor(onSurface);
        float left = UiUtil.dp(getContext(), 30), right = UiUtil.dp(getContext(), 12);
        float top = UiUtil.dp(getContext(), 22), bottom = UiUtil.dp(getContext(), 26);
        float w = getWidth() - left - right, h = getHeight() - top - bottom;
        if (w <= 0 || h <= 0) return;

        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            float y = top + h - h * i / steps;
            c.drawLine(left, y, left + w, y, grid);
            text.setTextAlign(Paint.Align.RIGHT);
            c.drawText(String.format("%.1f", yMax * i / steps), left - 6, y + text.getTextSize() / 3, text);
        }
        c.drawLine(left, top, left, top + h, axis);
        c.drawLine(left, top + h, left + w, top + h, axis);

        int n = labels.size();
        if (n == 0) {
            text.setTextAlign(Paint.Align.CENTER);
            c.drawText("Add semesters and grades to see your progress", left + w / 2, top + h / 2, text);
            return;
        }
        float stepX = n == 1 ? 0 : w / (n - 1);
        text.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            float x = n == 1 ? left + w / 2 : left + stepX * i;
            String l = labels.get(i);
            if (l.length() > 8) l = l.substring(0, 7) + "…";
            c.drawText(l, x, top + h + text.getTextSize() + 6, text);
        }
        for (int s = 0; s < series.size(); s++) {
            List<Double> vals = series.get(s);
            line.setColor(colors[s % colors.length]);
            dot.setColor(colors[s % colors.length]);
            Path p = new Path();
            for (int i = 0; i < vals.size(); i++) {
                float x = n == 1 ? left + w / 2 : left + stepX * i;
                float y = (float) (top + h - h * Math.min(vals.get(i), yMax) / yMax);
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
                c.drawCircle(x, y, UiUtil.dp(getContext(), 4), dot);
                text.setTextAlign(Paint.Align.CENTER);
                if (s == series.size() - 1 || series.size() == 1)
                    c.drawText(String.format("%.2f", vals.get(i)), x, y - UiUtil.dp(getContext(), 8), text);
            }
            c.drawPath(p, line);
        }
        // legend
        float lx = left;
        text.setTextAlign(Paint.Align.LEFT);
        for (int s = 0; s < names.size(); s++) {
            dot.setColor(colors[s % colors.length]);
            c.drawCircle(lx + 6, top - 10, 6, dot);
            c.drawText(names.get(s), lx + 18, top - 6, text);
            lx += text.measureText(names.get(s)) + 40;
        }
    }
}
