package com.aura.launcher;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lazy, size-bounded icon cache. Icons are rendered once onto a rounded plate
 * (consistent iOS-style shape) on a single background thread, held in a 2 MB LRU,
 * and dropped on memory pressure.
 */
class IconCache {
    final PackageManager pm;
    final Handler ui = new Handler(Looper.getMainLooper());
    final ExecutorService ex = Executors.newSingleThreadExecutor();
    final HashMap<String, ArrayList<View>> pending = new HashMap<String, ArrayList<View>>();
    final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(2 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };
    int size;

    IconCache(PackageManager pm) { this.pm = pm; }

    void setSize(int s) { if (s != size) { size = s; cache.evictAll(); } }
    void clear() { cache.evictAll(); }
    Bitmap get(String comp) { return cache.get(comp); }

    /** Request an icon; every view waiting on it is invalidated when it is ready. */
    void load(final String comp, View v) {
        ArrayList<View> l = pending.get(comp);
        if (l != null) { if (!l.contains(v)) l.add(v); return; }
        l = new ArrayList<View>();
        l.add(v);
        pending.put(comp, l);
        final int s = size;
        ex.execute(new Runnable() {
            public void run() {
                Drawable d = null;
                try { d = pm.getActivityIcon(ComponentName.unflattenFromString(comp)); } catch (Exception e) { }
                final Bitmap b = render(d, s);
                ui.post(new Runnable() {
                    public void run() {
                        if (s == size) cache.put(comp, b);
                        ArrayList<View> vs = pending.remove(comp);
                        if (vs != null) for (View x : vs) x.invalidate();
                    }
                });
            }
        });
    }

    static Bitmap render(Drawable d, int s) {
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, s, 0xFFFFFFFF, 0xFFE2E4EA, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0, 0, s, s), s * 0.225f, s * 0.225f, p);
        if (d != null) {
            int in = (int) (s * 0.15f);
            d.setBounds(in, in, s - in, s - in);
            d.draw(c);
        }
        return b;
    }

    void shutdown() { ex.shutdownNow(); }
}
