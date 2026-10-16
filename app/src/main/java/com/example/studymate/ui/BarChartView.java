package com.example.studymate.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/** Minimal bar chart used for study-time statistics. */
public class BarChartView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<String> labels = new ArrayList<>();
    private List<Float> values = new ArrayList<>();
    private List<Integer> colors = new ArrayList<>();
    private String unit = "";

    public BarChartView(Context c, AttributeSet a) {
        super(c, a);
        text.setTextSize(UiUtil.dp(c, 11));
        axis.setStrokeWidth(2f);
    }

    public void setData(List<String> labels, List<Float> values, List<Integer> colors, String unit) {
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        this.unit = unit;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        int onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        text.setColor(onSurface);
        axis.setColor(onSurface);
        float pad = UiUtil.dp(getContext(), 12);
        float top = UiUtil.dp(getContext(), 24), bottom = UiUtil.dp(getContext(), 30);
        float w = getWidth() - 2 * pad, h = getHeight() - top - bottom;
        if (w <= 0 || h <= 0) return;
        c.drawLine(pad, top + h, pad + w, top + h, axis);
        int n = values.size();
        if (n == 0) {
            text.setTextAlign(Paint.Align.CENTER);
            c.drawText("No study sessions yet", pad + w / 2, top + h / 2, text);
            return;
        }
        float max = 1f;
        for (float v : values) max = Math.max(max, v);
        float slot = w / n;
        float bw = Math.min(slot * 0.6f, UiUtil.dp(getContext(), 48));
        text.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            float cx = pad + slot * i + slot / 2;
            float bh = h * values.get(i) / max;
            bar.setColor(colors.get(i));
            c.drawRoundRect(cx - bw / 2, top + h - bh, cx + bw / 2, top + h, 8, 8, bar);
            c.drawText(Math.round(values.get(i)) + unit, cx, top + h - bh - 6, text);
            String l = labels.get(i);
            if (l.length() > 9) l = l.substring(0, 8) + "…";
            c.drawText(l, cx, top + h + text.getTextSize() + 6, text);
        }
    }
}
