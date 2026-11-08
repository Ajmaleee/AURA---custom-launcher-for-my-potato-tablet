package com.aura.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/** All user settings, persisted in SharedPreferences. */
class Prefs {
    final SharedPreferences sp;
    final float density;
    int cols, iconDp, gapDp;
    boolean labels, dock, clock, dark, anim;

    Prefs(Context c) {
        sp = c.getSharedPreferences("aura", Context.MODE_PRIVATE);
        density = c.getResources().getDisplayMetrics().density;
        cols = sp.getInt("cols", 6);
        iconDp = sp.getInt("icon", 64);
        gapDp = sp.getInt("gap", 8);
        labels = sp.getBoolean("labels", true);
        dock = sp.getBoolean("dock", true);
        clock = sp.getBoolean("clock", true);
        dark = sp.getBoolean("dark", true);
        anim = sp.getBoolean("anim", true);
    }

    void save() {
        sp.edit().putInt("cols", cols).putInt("icon", iconDp).putInt("gap", gapDp)
                .putBoolean("labels", labels).putBoolean("dock", dock).putBoolean("clock", clock)
                .putBoolean("dark", dark).putBoolean("anim", anim).apply();
    }

    int px(float dp) { return (int) (dp * density + 0.5f); }
}
