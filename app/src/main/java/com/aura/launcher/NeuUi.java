package com.aura.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * Soft-UI (neumorphic) background: raised or sunken. Rendered once per size with blurred
 * light/dark shadows into a bitmap, so drawing is a single blit. Its padding (m) keeps
 * content clear of the shadow area.
 */
class Neu extends Drawable {
    final int base, hi, lo, m, blur, off;
    final float radius;  // px; negative = pill
    final boolean inset;
    Bitmap bmp;
    int bw, bh;

    Neu(Prefs p, int base, int hi, int lo, float radiusDp, boolean inset, float depthDp) {
        this.base = base;
        this.hi = hi;
        this.lo = lo;
        this.inset = inset;
        this.radius = radiusDp < 0 ? -1 : p.px(radiusDp);
        this.m = p.px(depthDp * 2);
        this.blur = Math.max(1, p.px(depthDp * 1.6f));
        this.off = Math.max(1, p.px(depthDp));
    }

    // theme palette
    static int base(Prefs p) { return p.dark ? 0xFF2B2D33 : 0xFFE4E6EB; }
    static int hi(Prefs p) { return p.dark ? 0xFF3C3F48 : 0xFFFFFFFF; }
    static int lo(Prefs p) { return p.dark ? 0xFF191A1E : 0xFFB8BBC6; }
    static int fg(Prefs p) { return p.dark ? 0xFFE6E7EC : 0xFF3B3E47; }

    static Neu raised(Prefs p, float rDp, float depth) {
        return new Neu(p, base(p), hi(p), lo(p), rDp, false, depth);
    }

    static Neu sunk(Prefs p, float rDp, float depth) {
        return new Neu(p, base(p), hi(p), lo(p), rDp, true, depth);
    }

    /** Raised at rest, sunken while pressed. */
    static StateListDrawable button(Prefs p, float rDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, sunk(p, rDp, 2));
        s.addState(new int[]{}, raised(p, rDp, 2));
        return s;
    }

    static RectF shift(RectF r, float dx, float dy) {
        return new RectF(r.left + dx, r.top + dy, r.right + dx, r.bottom + dy);
    }

    static int mix(int a, int b, float t) {
        int ra = (a >> 24) & 255, rr = (a >> 16) & 255, rg = (a >> 8) & 255, rb = a & 255;
        int sa = (b >> 24) & 255, sr = (b >> 16) & 255, sg = (b >> 8) & 255, sb = b & 255;
        return ((int) (ra + (sa - ra) * t) << 24) | ((int) (rr + (sr - rr) * t) << 16)
                | ((int) (rg + (sg - rg) * t) << 8) | (int) (rb + (sb - rb) * t);
    }

    static Path frame(RectF body, float r, float dx, float dy, int w, int h) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.addRect(-w, -h, 2 * w, 2 * h, Path.Direction.CW);
        path.addRoundRect(shift(body, dx, dy), r, r, Path.Direction.CW);
        return path;
    }

    void render() {
        Rect b = getBounds();
        bw = b.width();
        bh = b.height();
        if (bw <= 2 * m || bh <= 2 * m) { bmp = null; return; }
        bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF body = new RectF(m, m, bw - m, bh - m);
        float r = radius < 0 ? Math.min(body.width(), body.height()) / 2f : radius;
        if (!inset) {
            pt.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
            pt.setColor(lo);
            c.drawRoundRect(shift(body, off, off), r, r, pt);
            pt.setColor(hi);
            c.drawRoundRect(shift(body, -off, -off), r, r, pt);
            pt.setMaskFilter(null);
            pt.setShader(new LinearGradient(body.left, body.top, body.right, body.bottom,
                    mix(base, hi, 0.3f), mix(base, lo, 0.18f), Shader.TileMode.CLAMP));
            c.drawRoundRect(body, r, r, pt);
        } else {
            pt.setShader(new LinearGradient(body.left, body.top, body.right, body.bottom,
                    mix(base, lo, 0.12f), mix(base, hi, 0.2f), Shader.TileMode.CLAMP));
            c.drawRoundRect(body, r, r, pt);
            pt.setShader(null);
            pt.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
            pt.setColor(lo);
            c.drawPath(frame(body, r, off, off, bw, bh), pt);
            pt.setColor(hi);
            c.drawPath(frame(body, r, -off, -off, bw, bh), pt);
            pt.setMaskFilter(null);
            Path mask = new Path();  // keep only the inside of the shape
            mask.addRoundRect(body, r, r, Path.Direction.CW);
            mask.setFillType(Path.FillType.INVERSE_WINDING);
            pt.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            c.drawPath(mask, pt);
        }
    }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        if (bmp == null || bw != b.width() || bh != b.height()) render();
        if (bmp != null) c.drawBitmap(bmp, b.left, b.top, null);
    }

    @Override protected void onBoundsChange(Rect r) { bmp = null; }
    @Override public boolean getPadding(Rect out) { out.set(m, m, m, m); return true; }
    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter f) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

/** Soft-UI toggle: sunken track, raised knob. */
class NeuSwitch extends View {
    interface Listener { void onChange(boolean on); }

    final Prefs p;
    final Neu track, knob;
    final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    final int w, h, k;
    boolean on;
    final Listener l;

    NeuSwitch(Context c, Prefs p, boolean on, Listener l) {
        super(c);
        this.p = p;
        this.on = on;
        this.l = l;
        track = Neu.sunk(p, -1, 2);
        knob = Neu.raised(p, -1, 2);
        w = p.px(54);
        h = p.px(30);
        k = p.px(26);
        track.setBounds(0, 0, w, h);
        setClickable(true);
    }

    @Override protected void onMeasure(int a, int b) { setMeasuredDimension(w, h); }

    @Override protected void onDraw(Canvas c) {
        track.draw(c);
        int kl = on ? w - k - p.px(2) : p.px(2);
        int kt = (h - k) / 2;
        knob.setBounds(kl, kt, kl + k, kt + k);
        knob.draw(c);
        dot.setColor(on ? 0xFF34C759 : 0xFF8E919A);
        c.drawCircle(kl + k / 2f, kt + k / 2f, p.px(4), dot);
    }

    @Override public boolean performClick() {
        on = !on;
        invalidate();
        if (l != null) l.onChange(on);
        return super.performClick();
    }
}

/** Soft-UI slider: sunken track, accent fill, raised knob. The listener fires when you let go. */
class NeuSlider extends View {
    interface Listener { void onStop(int value); }

    final Prefs p;
    final int min, max, w, h, k;
    final Neu track, knob;
    final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF rf = new RectF();
    final Listener l;
    int value;

    NeuSlider(Context c, Prefs p, int min, int max, int value, Listener l) {
        super(c);
        this.p = p;
        this.min = min;
        this.max = max;
        this.value = value;
        this.l = l;
        w = p.px(160);
        h = p.px(30);
        k = p.px(28);
        track = Neu.sunk(p, -1, 2);
        knob = Neu.raised(p, -1, 2);
        track.setBounds(0, (h - p.px(14)) / 2, w, (h - p.px(14)) / 2 + p.px(14));
    }

    @Override protected void onMeasure(int a, int b) { setMeasuredDimension(w, h); }

    @Override protected void onDraw(Canvas c) {
        track.draw(c);
        int kl = (int) ((w - k) * (value - min) / (float) (max - min));
        pt.setColor(0xFF2F7CF6);
        rf.set(p.px(4), h / 2f - p.px(3), kl + k / 2f, h / 2f + p.px(3));
        c.drawRoundRect(rf, p.px(3), p.px(3), pt);
        knob.setBounds(kl, (h - k) / 2, kl + k, (h - k) / 2 + k);
        knob.draw(c);
    }

    void set(float x) {
        float t = (x - k / 2f) / (w - k);
        t = Math.max(0f, Math.min(1f, t));
        value = min + Math.round(t * (max - min));
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                set(e.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                set(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
                set(e.getX());
                if (l != null) l.onStop(value);
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (l != null) l.onStop(value);
                return true;
        }
        return true;
    }
}

/**
 * Bottom sheet that follows the finger when pulled down (from its header, or from the list
 * when the list is scrolled to the top) and closes on a long pull or a downward flick.
 */
class Sheet extends LinearLayout {
    final Prefs p;
    final View scroller;
    final Runnable close;
    final int slop;
    float rawDown, downX;
    boolean drag;
    VelocityTracker vt;

    Sheet(Context c, Prefs p, View scroller, Runnable close) {
        super(c);
        this.p = p;
        this.scroller = scroller;
        this.close = close;
        setOrientation(VERTICAL);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
    }

    void track(MotionEvent e, boolean first) {
        if (first) {
            if (vt != null) vt.recycle();
            vt = VelocityTracker.obtain();
        }
        if (vt != null) vt.addMovement(e);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                rawDown = e.getRawY();
                drag = false;
                track(e, true);
                break;
            case MotionEvent.ACTION_MOVE: {
                track(e, false);
                float dy = e.getRawY() - rawDown;
                if (!drag && dy > slop && dy > Math.abs(e.getX() - downX) && !scroller.canScrollVertically(-1)) {
                    drag = true;
                    rawDown = e.getRawY();
                }
                return drag;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                drag = false;
                break;
        }
        return false;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:  // touch landed on the header / empty area
                downX = e.getX();
                rawDown = e.getRawY();
                track(e, true);
                return true;
            case MotionEvent.ACTION_MOVE: {
                track(e, false);
                float dy = e.getRawY() - rawDown;
                if (!drag && dy > slop) {
                    drag = true;
                    rawDown = e.getRawY();
                }
                if (drag) setTranslationY(Math.max(0f, e.getRawY() - rawDown));
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                track(e, false);
                if (drag && vt != null) {
                    vt.computeCurrentVelocity(1000);
                    finish(vt.getYVelocity());
                }
                drag = false;
                if (vt != null) { vt.recycle(); vt = null; }
                return true;
        }
        return true;
    }

    void finish(float vy) {
        if (getTranslationY() > getHeight() * 0.22f || vy > 900) close.run();
        else if (p.anim) animate().translationY(0).setDuration(160);
        else setTranslationY(0);
    }
}