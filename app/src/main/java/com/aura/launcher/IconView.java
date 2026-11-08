package com.aura.launcher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.View;

/**
 * One icon + label drawn straight onto a canvas (no child views).
 * Used by the home grid, the dock, the app library and folders.
 */
class IconView extends View {
    final Prefs p;
    final IconCache ic;
    final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF rf = new RectF();
    Item e;
    int idx;
    boolean edit, noLabel;
    int labelColor = Color.WHITE;
    String ell;
    int ellW;
    ObjectAnimator jig;

    IconView(Context c, Prefs p, IconCache ic) {
        super(c);
        this.p = p;
        this.ic = ic;
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(p.px(12));
    }

    void bind(Item e) { this.e = e; ell = null; invalidate(); }

    /** Panels draw dark text without a shadow; the wallpaper uses white text with one. */
    void setLabelStyle(int color, boolean shadow) {
        labelColor = color;
        if (shadow) tp.setShadowLayer(2, 0, 1, 0x99000000);
    }

    @Override public void setPressed(boolean pr) {
        super.setPressed(pr);
        setScaleX(pr ? 0.92f : 1f);
        setScaleY(pr ? 0.92f : 1f);
    }

    void jiggle(boolean on) {
        if (on) {
            if (jig == null) {
                jig = ObjectAnimator.ofFloat(this, "rotation", -1.6f, 1.6f);
                jig.setDuration(120 + (int) (Math.random() * 40));
                jig.setRepeatCount(ValueAnimator.INFINITE);
                jig.setRepeatMode(ValueAnimator.REVERSE);
            }
            if (!jig.isRunning()) jig.start();
        } else {
            if (jig != null) { jig.cancel(); jig = null; }
            setRotation(0);
        }
    }

    @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); jiggle(false); }

    @Override protected void onDraw(Canvas c) {
        if (e == null) return;
        int w = getWidth(), s = ic.size, l = (w - s) / 2;
        if (e.isFolder()) {
            pt.setColor(0x55FFFFFF);
            rf.set(l, 0, l + s, s);
            c.drawRoundRect(rf, s * 0.225f, s * 0.225f, pt);
            pt.setColor(Color.WHITE);
            float m = s * 0.36f, g = s * 0.09f;
            for (int k = 0; k < 4 && k < e.kids.size(); k++) {
                Bitmap b = ic.get(e.kids.get(k));
                if (b == null) { ic.load(e.kids.get(k), this); continue; }
                float x = l + g + (k % 2) * (m + g * 0.6f), y = g + (k / 2) * (m + g * 0.6f);
                rf.set(x, y, x + m, y + m);
                c.drawBitmap(b, null, rf, pt);
            }
        } else {
            Bitmap b = ic.get(e.comp);
            if (b != null) c.drawBitmap(b, l, 0, null);
            else {
                pt.setColor(0x33FFFFFF);
                rf.set(l, 0, l + s, s);
                c.drawRoundRect(rf, s * 0.225f, s * 0.225f, pt);
                ic.load(e.comp, this);
            }
        }
        if (!noLabel && p.labels) {
            if (ell == null || ellW != w) {
                String n = e.isFolder() ? e.name : e.label;
                ell = TextUtils.ellipsize(n, tp, w - p.px(6), TextUtils.TruncateAt.END).toString();
                ellW = w;
            }
            tp.setColor(labelColor);
            c.drawText(ell, w / 2f, s + p.px(16), tp);
        }
        if (edit) {  // remove badge
            float r = p.px(9);
            pt.setColor(0xFFE9E9EE);
            float bx = l + r - p.px(2);
            c.drawCircle(bx, r, r, pt);
            pt.setColor(0xFF333333);
            c.drawRect(bx - r * 0.5f, r - 1, bx + r * 0.5f, r + 1, pt);
        }
    }
}
