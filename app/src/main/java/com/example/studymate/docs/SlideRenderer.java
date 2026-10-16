package com.example.studymate.docs;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

/** Draws a slide (roughly keeping its layout) onto any Canvas: screen bitmap or PDF page. */
public final class SlideRenderer {
    private SlideRenderer() {}

    public static Bitmap renderBitmap(PptxModel m, PptxModel.Slide s, int widthPx) {
        int h = Math.max(1, Math.round(widthPx * (m.heightEmu / (float) m.widthEmu)));
        Bitmap bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888);
        draw(m, s, new Canvas(bmp), widthPx, h, Math.max(1000, widthPx));
        return bmp;
    }

    public static void draw(PptxModel m, PptxModel.Slide s, Canvas c, float w, float h, int imgMaxDim) {
        c.drawColor(Color.WHITE);
        float ex = w / m.widthEmu, ey = h / m.heightEmu;
        float pxPerPt = w / (m.widthEmu / 12700f);

        int bodyCount = 0;
        for (PptxModel.Shape sh : s.shapes) {
            if (!hasPosition(sh) && !sh.image && !sh.isTitle()) bodyCount++;
        }
        int bodyIdx = 0;

        for (PptxModel.Shape sh : s.shapes) {
            RectF r;
            if (hasPosition(sh)) {
                r = new RectF(sh.x * ex, sh.y * ey, (sh.x + sh.w) * ex, (sh.y + sh.h) * ey);
            } else if (sh.image) {
                r = new RectF(w * 0.1f, h * 0.25f, w * 0.9f, h * 0.92f);
            } else if (sh.isTitle()) {
                r = new RectF(w * 0.05f, h * 0.04f, w * 0.95f, h * 0.22f);
            } else {
                float top = h * 0.25f, bottom = h * 0.95f;
                float slot = (bottom - top) / Math.max(1, bodyCount);
                r = new RectF(w * 0.07f, top + slot * bodyIdx, w * 0.93f, top + slot * (bodyIdx + 1));
                bodyIdx++;
            }
            if (sh.image) drawImage(m, sh, c, r, imgMaxDim);
            else drawText(sh, c, r, pxPerPt);
        }
    }

    private static boolean hasPosition(PptxModel.Shape sh) {
        return sh.x >= 0 && sh.y >= 0 && sh.w > 0 && sh.h > 0;
    }

    private static void drawImage(PptxModel m, PptxModel.Shape sh, Canvas c, RectF r, int maxDim) {
        Bitmap bmp = m.loadImage(sh.mediaPath, maxDim);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (bmp == null) {
            paint.setColor(0xFFE0E0E0);
            c.drawRect(r, paint);
            return;
        }
        float scale = Math.min(r.width() / bmp.getWidth(), r.height() / bmp.getHeight());
        float dw = bmp.getWidth() * scale, dh = bmp.getHeight() * scale;
        float left = r.left + (r.width() - dw) / 2, top = r.top + (r.height() - dh) / 2;
        c.drawBitmap(bmp, null, new RectF(left, top, left + dw, top + dh), paint);
        bmp.recycle();
    }

    private static List<StaticLayout> layouts(PptxModel.Shape sh, float width, float pxPerPt, float scale, float[] totalOut) {
        List<StaticLayout> list = new ArrayList<>();
        float total = 0;
        for (PptxModel.Para p : sh.paras) {
            float pt = p.sizePt > 0 ? p.sizePt : (sh.isTitle() ? 30f : 18f);
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(Color.BLACK);
            tp.setTextSize(pt * pxPerPt * scale);
            boolean bold = p.bold || sh.isTitle();
            tp.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            String text = (p.bullet ? "• " : "") + p.text;
            StaticLayout sl = StaticLayout.Builder
                    .obtain(text, 0, text.length(), tp, Math.max(1, (int) width))
                    .setAlignment(sh.isTitle() ? Layout.Alignment.ALIGN_CENTER : Layout.Alignment.ALIGN_NORMAL)
                    .build();
            list.add(sl);
            total += sl.getHeight() + 4 * pxPerPt * scale;
        }
        totalOut[0] = total;
        return list;
    }

    private static void drawText(PptxModel.Shape sh, Canvas c, RectF r, float pxPerPt) {
        float padX = 7.2f * pxPerPt, padY = 3.6f * pxPerPt;
        float width = r.width() - 2 * padX, height = r.height() - 2 * padY;
        if (width <= 4 || height <= 4) return;
        float scale = 1f;
        float[] total = new float[1];
        List<StaticLayout> ls = layouts(sh, width, pxPerPt, scale, total);
        for (int i = 0; i < 8 && total[0] > height && scale > 0.4f; i++) {
            scale *= 0.88f;
            ls = layouts(sh, width, pxPerPt, scale, total);
        }
        c.save();
        c.clipRect(r);
        c.translate(r.left + padX, r.top + padY);
        for (StaticLayout sl : ls) {
            sl.draw(c);
            c.translate(0, sl.getHeight() + 4 * pxPerPt * scale);
        }
        c.restore();
    }
}
