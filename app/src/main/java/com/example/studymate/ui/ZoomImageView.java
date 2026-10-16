package com.example.studymate.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/** ImageView with pinch-zoom, pan, double-tap zoom and horizontal fling (page turn) at base zoom. */
public class ZoomImageView extends AppCompatImageView {

    public interface SwipeListener {
        void onSwipe(boolean next);
    }

    public SwipeListener swipeListener;

    private final Matrix matrix = new Matrix();
    private final float[] v = new float[9];
    private float baseScale = 1f;
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    public ZoomImageView(Context c, AttributeSet a) {
        super(c, a);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float cur = curScale();
                float target = Math.max(baseScale, Math.min(baseScale * 5f, cur * d.getScaleFactor()));
                float s = target / cur;
                matrix.postScale(s, s, d.getFocusX(), d.getFocusY());
                apply();
                return true;
            }
        });
        gestureDetector = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (curScale() > baseScale + 0.01f) {
                    matrix.postTranslate(-dx, -dy);
                    apply();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float cur = curScale();
                float target = cur > baseScale + 0.01f ? baseScale : baseScale * 2.5f;
                float s = target / cur;
                matrix.postScale(s, s, e.getX(), e.getY());
                apply();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (curScale() <= baseScale + 0.01f && swipeListener != null
                        && Math.abs(vx) > 1200 && Math.abs(vx) > Math.abs(vy)) {
                    swipeListener.onSwipe(vx < 0);
                    return true;
                }
                return false;
            }
        });
    }

    private float curScale() {
        matrix.getValues(v);
        return v[Matrix.MSCALE_X];
    }

    private void apply() {
        fixBounds();
        setImageMatrix(matrix);
    }

    private void fixBounds() {
        if (getDrawable() == null) return;
        RectF r = new RectF(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        matrix.mapRect(r);
        float dx = 0, dy = 0;
        if (r.width() <= getWidth()) dx = (getWidth() - r.width()) / 2 - r.left;
        else if (r.left > 0) dx = -r.left;
        else if (r.right < getWidth()) dx = getWidth() - r.right;
        if (r.height() <= getHeight()) dy = (getHeight() - r.height()) / 2 - r.top;
        else if (r.top > 0) dy = -r.top;
        else if (r.bottom < getHeight()) dy = getHeight() - r.bottom;
        matrix.postTranslate(dx, dy);
    }

    public void resetZoom() {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) return;
        float dw = getDrawable().getIntrinsicWidth(), dh = getDrawable().getIntrinsicHeight();
        baseScale = Math.min(getWidth() / dw, getHeight() / dh);
        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        apply();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        resetZoom();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        resetZoom();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        if (!scaleDetector.isInProgress()) gestureDetector.onTouchEvent(e);
        return true;
    }
}
