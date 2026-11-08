package com.aura.launcher;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
import java.util.ArrayList;

/**
 * Paged icon grid. Items are one flat list; pages are computed at layout time, so
 * rotation, column count and icon size never corrupt the layout. Handles paging
 * swipes, swipe-up (library), long-press (edit / settings), drag reorder and folder creation.
 */
class HomeView extends ViewGroup {
    final LauncherActivity a;
    final Prefs p;
    final IconCache ic;
    final ArrayList<Item> items;
    final Scroller sc;
    final int slop;
    final Handler h = new Handler();
    final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    int cols = 1, rows = 1, cap = 1, pages = 1, page = 0, cellW = 1, cellH = 1, margin;
    float downX, downY, lastX;
    int mode;  // 0 pending, 1 paging, 2 dragging, 3 ignore
    VelocityTracker vt;
    IconView target, dragV;
    boolean edit;

    final Runnable lp = new Runnable() {
        public void run() {
            if (mode != 0) return;
            if (target != null) { if (!edit) a.setEdit(true); startDrag(target); }
            else a.openSettings();
        }
    };

    HomeView(Context c, LauncherActivity a, Prefs p, IconCache ic, ArrayList<Item> items) {
        super(c);
        this.a = a; this.p = p; this.ic = ic; this.items = items;
        sc = new Scroller(c);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        setClipChildren(false);
    }

    void rebuild() {
        removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            IconView v = new IconView(getContext(), p, ic);
            v.idx = i;
            v.bind(items.get(i));
            v.edit = edit;
            if (edit) v.jiggle(p.anim);
            addView(v);
        }
        requestLayout();
        invalidate();
    }

    void setEdit(boolean on) {
        edit = on;
        for (int i = 0; i < getChildCount(); i++) {
            IconView v = (IconView) getChildAt(i);
            v.edit = on;
            v.jiggle(on && p.anim);
            v.invalidate();
        }
    }

    void grid(int w, int hh) {
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        cols = land ? p.cols : Math.max(3, (p.cols * 2 + 2) / 3);
        margin = p.px(24);
        cellW = Math.max(1, (w - 2 * margin) / cols);
        cellH = ic.size + p.px(p.labels ? 22 : 8) + p.px(p.gapDp);
        rows = Math.max(1, (hh - p.px(22)) / cellH);
        cap = cols * rows;
        pages = Math.max(1, (items.size() + cap - 1) / cap);
        if (page >= pages) page = pages - 1;
    }

    @Override protected void onMeasure(int wm, int hm) {
        int w = MeasureSpec.getSize(wm), hh = MeasureSpec.getSize(hm);
        setMeasuredDimension(w, hh);
        grid(w, hh);
        int cw = MeasureSpec.makeMeasureSpec(cellW, MeasureSpec.EXACTLY);
        int ch = MeasureSpec.makeMeasureSpec(cellH, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).measure(cw, ch);
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        for (int i = 0; i < getChildCount(); i++) {
            IconView v = (IconView) getChildAt(i);
            int k = v.idx % cap, left = (v.idx / cap) * w + margin + (k % cols) * cellW, top = (k / cols) * cellH;
            v.layout(left, top, left + cellW, top + cellH);
        }
        if (sc.isFinished()) scrollTo(page * w, 0);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) { return true; }

    IconView hit(float x, float y) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View c = getChildAt(i);
            if (x >= c.getLeft() && x < c.getRight() && y >= c.getTop() && y < c.getBottom()) return (IconView) c;
        }
        return null;
    }

    void startDrag(IconView v) {
        mode = 2;
        dragV = v;
        v.bringToFront();
        v.setScaleX(1.12f);
        v.setScaleY(1.12f);
        v.setAlpha(0.9f);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = e.getY();
                mode = 0;
                if (!sc.isFinished()) { sc.abortAnimation(); mode = 1; }
                vt = VelocityTracker.obtain();
                vt.addMovement(e);
                target = hit(e.getX() + getScrollX(), e.getY());
                if (target != null && !edit) target.setPressed(true);
                h.postDelayed(lp, 550);
                break;
            case MotionEvent.ACTION_MOVE: {
                vt.addMovement(e);
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (mode == 2) { dragV.setTranslationX(dx); dragV.setTranslationY(dy); break; }
                if (mode == 0 && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
                    h.removeCallbacks(lp);
                    if (target != null) target.setPressed(false);
                    if (edit && target != null) startDrag(target);
                    else if (Math.abs(dx) > Math.abs(dy)) mode = 1;
                    else if (dy < 0 && !edit) { mode = 3; a.openDrawer(); }
                    else mode = 3;
                }
                if (mode == 1) {
                    int nx = getScrollX() + (int) (lastX - e.getX());
                    int max = (pages - 1) * getWidth(), over = getWidth() / 8;
                    scrollTo(Math.max(-over, Math.min(max + over, nx)), 0);
                }
                lastX = e.getX();
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                h.removeCallbacks(lp);
                if (target != null) target.setPressed(false);
                if (mode == 2) finishDrag(e.getX() + getScrollX(), e.getY(), e.getRawX());
                else if (mode == 1) {
                    vt.computeCurrentVelocity(1000);
                    float vx = vt.getXVelocity();
                    int pg = Math.round((float) getScrollX() / getWidth());
                    if (vx < -700) pg = page + 1; else if (vx > 700) pg = page - 1;
                    snap(pg);
                } else if (mode == 0 && e.getActionMasked() == MotionEvent.ACTION_UP) tap(e.getX() + getScrollX(), e.getY());
                if (vt != null) { vt.recycle(); vt = null; }
                mode = 0;
                target = null;
                break;
        }
        return true;
    }

    void tap(float x, float y) {
        IconView v = hit(x, y);
        if (v == null) return;
        if (edit) {
            float lx = x - v.getLeft();
            if (lx < (v.getWidth() - ic.size) / 2f + p.px(22) && y - v.getTop() < p.px(22)) {
                items.remove(v.e);
                a.persist();
                rebuild();
            }
        } else a.activate(v.e);
    }

    void finishDrag(float cx, float cy, float rawX) {
        Item d = dragV.e;
        dragV.setTranslationX(0); dragV.setTranslationY(0);
        dragV.setScaleX(1); dragV.setScaleY(1); dragV.setAlpha(1);
        dragV = null;
        if (items.isEmpty()) return;
        if (cy > getHeight() && a.dockDrop(d, rawX)) return;
        int w = getWidth(), pg = Math.round((float) getScrollX() / w);
        int col = Math.max(0, Math.min(cols - 1, (int) ((cx - pg * w - margin) / cellW)));
        int row = Math.max(0, Math.min(rows - 1, (int) (cy / cellH)));
        int tgt = Math.min(items.size() - 1, pg * cap + row * cols + col);
        Item t = items.get(tgt);
        if (t != d) {
            float cxc = pg * w + margin + col * cellW + cellW / 2f, cyc = row * cellH + ic.size / 2f;
            boolean inner = Math.abs(cx - cxc) < ic.size * 0.3f && Math.abs(cy - cyc) < ic.size * 0.3f;
            if (inner && !d.isFolder()) {  // drop on an icon: make or join a folder
                if (t.isFolder()) t.kids.add(d.comp);
                else {
                    Item f = new Item();
                    f.name = "Folder";
                    f.kids = new ArrayList<String>();
                    f.kids.add(t.comp);
                    f.kids.add(d.comp);
                    items.set(tgt, f);
                }
                items.remove(d);
            } else {
                items.remove(d);
                items.add(Math.min(tgt, items.size()), d);
            }
        }
        a.persist();
        rebuild();
    }

    void snap(int pg) {
        pg = Math.max(0, Math.min(pages - 1, pg));
        page = pg;
        int dx = pg * getWidth() - getScrollX();
        sc.startScroll(getScrollX(), 0, dx, 0, p.anim ? Math.min(300, Math.abs(dx) / 2 + 80) : 1);
        invalidate();
    }

    @Override public void computeScroll() {
        if (sc.computeScrollOffset()) { scrollTo(sc.getCurrX(), 0); postInvalidate(); }
    }

    @Override protected void dispatchDraw(Canvas c) {
        super.dispatchDraw(c);
        if (pages < 2) return;
        int act = Math.round((float) getScrollX() / getWidth());
        float gap = p.px(14), x0 = getScrollX() + getWidth() / 2f - (pages - 1) * gap / 2f, y = getHeight() - p.px(9);
        for (int i = 0; i < pages; i++) {
            dot.setColor(i == act ? 0xFFFFFFFF : 0x66FFFFFF);
            c.drawCircle(x0 + i * gap, y, p.px(3.5f), dot);
        }
    }
}
