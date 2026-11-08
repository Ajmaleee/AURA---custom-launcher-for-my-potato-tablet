package com.aura.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/**
 * AURA: the single HOME activity. Overlays (library, folder, settings) are created
 * on demand and removed when closed, so nothing is held while they are not visible.
 * No services; receivers are registered only while they are needed.
 */
public class LauncherActivity extends Activity {
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    Prefs p;
    final Repo repo = new Repo();
    final Store st = new Store();
    IconCache ic;
    FrameLayout root;
    HomeView home;
    LinearLayout dockBar, clockBox;
    TextView clockT, dateT, done;
    View overlay;
    int kind;  // 1 library, 2 folder, 3 settings
    boolean edit;
    final SimpleDateFormat df = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());

    final View.OnClickListener close = new View.OnClickListener() {
        public void onClick(View v) { closeOverlay(true); }
    };

    final BroadcastReceiver tick = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) { updateClock(); }
    };

    final BroadcastReceiver pkgR = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            boolean replacing = i.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (Intent.ACTION_PACKAGE_REMOVED.equals(i.getAction()) && replacing) return;
            HashSet<String> old = new HashSet<String>(repo.map.keySet());
            repo.load(LauncherActivity.this);
            st.prune(repo);
            for (App a : repo.apps) if (!old.contains(a.comp) && !replacing) st.home.add(Item.app(a));
            persist();
            closeOverlay(false);
            home.rebuild();
            rebuildDock();
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        repo.load(this);
        st.load(this, p, repo);
        ic = new IconCache(getPackageManager());
        ic.setSize(p.px(p.iconDp));
        build();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(pkgR, f);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(pkgR);
        ic.shutdown();
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(tick, f);
        updateClock();
    }

    @Override protected void onStop() {
        super.onStop();
        unregisterReceiver(tick);
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) ic.clear();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        home.requestLayout();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (Intent.ACTION_MAIN.equals(i.getAction())) {  // Home button pressed
            closeOverlay(true);
            setEdit(false);
            home.snap(0);
        }
    }

    @Override public void onBackPressed() {
        if (overlay != null) closeOverlay(true);
        else if (edit) setEdit(false);
        // a launcher never finishes
    }

    // ------------------------------------------------------------------ UI

    void build() {
        root = new FrameLayout(this);
        root.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x22000000, 0x55000000}));
        setContentView(root);

        dockBar = new LinearLayout(this);
        dockBar.setGravity(Gravity.CENTER);
        GradientDrawable dg = new GradientDrawable();
        dg.setColor(0x66141824);
        dg.setCornerRadius(p.px(26));
        dockBar.setBackgroundDrawable(dg);
        dockBar.setPadding(p.px(14), p.px(10), p.px(14), p.px(10));
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dl.bottomMargin = p.px(10);
        root.addView(dockBar, dl);

        home = new HomeView(this, this, p, ic, st.home);  // added after the dock so dragged icons draw over it
        root.addView(home, new FrameLayout.LayoutParams(MATCH, MATCH));

        clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockT = new TextView(this);
        clockT.setTextSize(44);
        clockT.setTextColor(Color.WHITE);
        clockT.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clockT.setShadowLayer(3, 0, 1, 0x66000000);
        dateT = new TextView(this);
        dateT.setTextSize(14);
        dateT.setTextColor(0xE6FFFFFF);
        dateT.setShadowLayer(3, 0, 1, 0x66000000);
        clockBox.addView(clockT);
        clockBox.addView(dateT);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.LEFT);
        cl.leftMargin = p.px(48);
        cl.topMargin = p.px(8);
        root.addView(clockBox, cl);

        done = new TextView(this);
        done.setText("Done");
        done.setTextSize(15);
        done.setTypeface(Typeface.DEFAULT_BOLD);
        done.setTextColor(0xFF222222);
        done.setPadding(p.px(18), p.px(7), p.px(18), p.px(7));
        GradientDrawable db = new GradientDrawable();
        db.setColor(0xE6FFFFFF);
        db.setCornerRadius(p.px(16));
        done.setBackgroundDrawable(db);
        done.setVisibility(View.GONE);
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setEdit(false); }
        });
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.RIGHT);
        fl.topMargin = p.px(10);
        fl.rightMargin = p.px(20);
        root.addView(done, fl);

        layoutMargins();
        rebuildDock();
        home.rebuild();
    }

    void layoutMargins() {
        FrameLayout.LayoutParams l = (FrameLayout.LayoutParams) home.getLayoutParams();
        l.topMargin = p.clock ? p.px(96) : p.px(8);
        l.bottomMargin = p.dock ? ic.size + p.px(40) : p.px(8);
        home.setLayoutParams(l);
        clockBox.setVisibility(p.clock ? View.VISIBLE : View.GONE);
        dockBar.setVisibility(p.dock ? View.VISIBLE : View.GONE);
    }

    void rebuildDock() {
        dockBar.removeAllViews();
        for (final String comp : st.dock) {
            App a = repo.map.get(comp);
            if (a == null) continue;
            IconView v = new IconView(this, p, ic);
            v.noLabel = true;
            v.bind(Item.app(a));
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) { if (!edit) launch(comp); }
            });
            dockBar.addView(v, new LinearLayout.LayoutParams(ic.size + p.px(22), ic.size + p.px(2)));
        }
    }

    void updateClock() {
        Date d = new Date();
        clockT.setText(DateFormat.getTimeFormat(this).format(d));
        dateT.setText(df.format(d));
    }

    void setEdit(boolean on) {
        if (edit == on) return;
        edit = on;
        home.setEdit(on);
        done.setVisibility(on ? View.VISIBLE : View.GONE);
        if (!on) persist();
    }

    void persist() { st.save(p); }

    // ------------------------------------------------------------- actions

    void launch(String comp) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(ComponentName.unflattenFromString(comp));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Can't open this app", Toast.LENGTH_SHORT).show();
        }
    }

    void activate(Item e) {
        if (e.isFolder()) openFolder(e);
        else launch(e.comp);
    }

    /** A home icon was dropped on the dock: it replaces the nearest dock slot, which moves to home. */
    boolean dockDrop(Item d, float rawX) {
        if (!p.dock || d.isFolder() || st.dock.isEmpty()) return false;
        int[] loc = new int[2];
        dockBar.getLocationOnScreen(loc);
        int n = st.dock.size();
        int i = Math.max(0, Math.min(n - 1, (int) ((rawX - loc[0]) / Math.max(1, dockBar.getWidth()) * n)));
        String old = st.dock.get(i);
        st.dock.set(i, d.comp);
        int at = st.home.indexOf(d);
        App oa = repo.map.get(old);
        if (oa != null && at >= 0) st.home.set(at, Item.app(oa)); else st.home.remove(d);
        persist();
        home.rebuild();
        rebuildDock();
        return true;
    }

    void apply(boolean theme) {
        p.save();
        ic.setSize(p.px(p.iconDp));
        layoutMargins();
        rebuildDock();
        home.rebuild();
        if (theme && overlay != null && kind == 3) { root.removeView(overlay); overlay = null; showSettings(false); }
    }

    // ------------------------------------------------------------- overlays

    int bg() { return p.dark ? 0xF0232529 : 0xF0F6F6F8; }
    int fg() { return p.dark ? 0xFFF1F2F5 : 0xFF1B1C20; }

    TextView tv(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    void closeOverlay(boolean anim) {
        if (overlay == null) return;
        final View o = overlay;
        int k = kind;
        overlay = null;
        kind = 0;
        InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(root.getWindowToken(), 0);
        if (anim && p.anim) {
            ViewPropertyAnimator va = o.animate().setDuration(180);
            if (k == 1) va.translationY(root.getHeight());
            else if (k == 3) va.translationX(root.getWidth());
            else va.alpha(0);
            va.withEndAction(new Runnable() { public void run() { root.removeView(o); } });
        } else root.removeView(o);
    }

    /** One adapter for the library and for folder contents: a plain grid of icon + name. */
    class AppAdapter extends BaseAdapter {
        final ArrayList<App> src, list = new ArrayList<App>();
        AppAdapter(ArrayList<App> src) { this.src = src; filter(""); }
        void filter(String q) {
            list.clear();
            q = q.toLowerCase(Locale.getDefault()).trim();
            for (App a : src) if (q.length() == 0 || a.label.toLowerCase(Locale.getDefault()).contains(q)) list.add(a);
            notifyDataSetChanged();
        }
        public int getCount() { return list.size(); }
        public Object getItem(int i) { return list.get(i); }
        public long getItemId(int i) { return i; }
        public View getView(int i, View cv, ViewGroup parent) {
            IconView v = (IconView) cv;
            if (v == null) {
                v = new IconView(LauncherActivity.this, p, ic);
                v.setLabelStyle(fg(), false);
                v.setLayoutParams(new AbsListView.LayoutParams(MATCH, ic.size + p.px(30)));
            }
            v.bind(Item.app(list.get(i)));
            return v;
        }
    }

    GridView grid(AppAdapter ad, int colW) {
        GridView g = new GridView(this);
        g.setNumColumns(GridView.AUTO_FIT);
        g.setColumnWidth(colW);
        g.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        g.setVerticalSpacing(p.px(10));
        g.setSelector(new ColorDrawable(Color.TRANSPARENT));
        g.setAdapter(ad);
        return g;
    }

    void openDrawer() {
        if (overlay != null) return;
        setEdit(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(bg());
        float r = p.px(22);
        bgd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        col.setBackgroundDrawable(bgd);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(p.px(32), p.px(8), p.px(32), p.px(8));
        TextView title = tv("App Library", 22, fg());
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1));
        final EditText q = new EditText(this);
        q.setHint("Search apps");
        q.setSingleLine(true);
        q.setTextColor(fg());
        q.setHintTextColor(0xFF8E919A);
        head.addView(q, new LinearLayout.LayoutParams(p.px(300), WRAP));
        col.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));
        head.setOnTouchListener(new View.OnTouchListener() {  // drag the header down to close
            float y0;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) y0 = e.getY();
                else if (e.getAction() == MotionEvent.ACTION_UP && e.getY() - y0 > p.px(50)) closeOverlay(true);
                return false;
            }
        });

        final AppAdapter ad = new AppAdapter(repo.apps);
        GridView g = grid(ad, ic.size + p.px(36));
        g.setPadding(p.px(24), p.px(8), p.px(24), p.px(8));
        g.setClipToPadding(false);
        g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int i, long id) {
                String comp = ad.list.get(i).comp;
                closeOverlay(false);
                launch(comp);
            }
        });
        g.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> a, View v, int i, long id) {
                addToHome(ad.list.get(i));
                return true;
            }
        });
        col.addView(g, new LinearLayout.LayoutParams(MATCH, 0, 1));
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { ad.filter(s.toString()); }
            public void afterTextChanged(Editable s) { }
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH, MATCH);
        lp.topMargin = p.px(28);
        root.addView(col, lp);
        if (p.anim) {
            col.setTranslationY(root.getHeight());
            col.animate().translationY(0).setDuration(240).setInterpolator(new DecelerateInterpolator());
        }
        overlay = col;
        kind = 1;
    }

    void addToHome(App a) {
        for (Item i : st.home) if (!i.isFolder() && a.comp.equals(i.comp)) {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show();
            return;
        }
        st.home.add(Item.app(a));
        persist();
        home.rebuild();
        Toast.makeText(this, a.label + " added to Home", Toast.LENGTH_SHORT).show();
        closeOverlay(true);
    }

    void openFolder(final Item f) {
        if (overlay != null) return;
        FrameLayout scrim = new FrameLayout(this);
        scrim.setBackgroundColor(0x66000000);
        scrim.setOnClickListener(close);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClickable(true);
        GradientDrawable pd = new GradientDrawable();
        pd.setColor(bg());
        pd.setCornerRadius(p.px(28));
        panel.setBackgroundDrawable(pd);
        panel.setPadding(p.px(20), p.px(14), p.px(20), p.px(20));
        final TextView title = tv(f.name, 20, fg());
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, p.px(10));
        title.setOnClickListener(new View.OnClickListener() {  // tap the title to rename
            public void onClick(View v) {
                final EditText et = new EditText(LauncherActivity.this);
                et.setText(f.name);
                et.setSingleLine(true);
                new AlertDialog.Builder(LauncherActivity.this).setTitle("Rename folder").setView(et)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                String n = et.getText().toString().trim();
                                if (n.length() > 0) { f.name = n; title.setText(n); persist(); home.rebuild(); }
                            }
                        }).show();
            }
        });
        panel.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        ArrayList<App> kids = new ArrayList<App>();
        for (String k : f.kids) { App a = repo.map.get(k); if (a != null) kids.add(a); }
        final AppAdapter ad = new AppAdapter(kids);
        GridView g = grid(ad, ic.size + p.px(30));
        g.setNumColumns(3);
        int nrows = Math.max(1, Math.min(3, (kids.size() + 2) / 3));
        g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int i, long id) {
                String comp = ad.list.get(i).comp;
                closeOverlay(false);
                launch(comp);
            }
        });
        g.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {  // long-press: take out of folder
            public boolean onItemLongClick(AdapterView<?> a, View v, int i, long id) {
                App app = ad.list.get(i);
                f.kids.remove(app.comp);
                int at = st.home.indexOf(f);
                if (f.kids.size() <= 1 && at >= 0) {
                    if (f.kids.size() == 1 && repo.map.get(f.kids.get(0)) != null) st.home.set(at, Item.app(repo.map.get(f.kids.get(0))));
                    else st.home.remove(f);
                }
                st.home.add(Item.app(app));
                persist();
                home.rebuild();
                closeOverlay(true);
                return true;
            }
        });
        panel.addView(g, new LinearLayout.LayoutParams(MATCH, nrows * (ic.size + p.px(40))));
        scrim.addView(panel, new FrameLayout.LayoutParams(p.px(3 * (p.iconDp + 30) + 40 > 460 ? 3 * (p.iconDp + 30) + 40 : 460), WRAP, Gravity.CENTER));
        root.addView(scrim, new FrameLayout.LayoutParams(MATCH, MATCH));
        if (p.anim) {
            panel.setScaleX(0.6f); panel.setScaleY(0.6f); panel.setAlpha(0);
            panel.animate().scaleX(1).scaleY(1).alpha(1).setDuration(200).setInterpolator(new DecelerateInterpolator());
        }
        overlay = scrim;
        kind = 2;
    }

    // ------------------------------------------------------------- settings

    void openSettings() { if (overlay == null) showSettings(true); }

    LinearLayout row(String label, View ctl) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, p.px(6), 0, p.px(6));
        r.addView(tv(label, 15, fg()), new LinearLayout.LayoutParams(0, WRAP, 1));
        r.addView(ctl);
        return r;
    }

    Switch sw(boolean v, final int k) {
        Switch s = new Switch(this);
        s.setChecked(v);
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                switch (k) {
                    case 0: p.labels = on; break;
                    case 1: p.dock = on; break;
                    case 2: p.clock = on; break;
                    case 3: p.dark = on; break;
                    default: p.anim = on;
                }
                apply(k == 3);
            }
        });
        return s;
    }

    SeekBar sb(int max, int prog, final int k) {
        SeekBar s = new SeekBar(this);
        s.setMax(max);
        s.setProgress(prog);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int pr, boolean u) { }
            public void onStartTrackingTouch(SeekBar b) { }
            public void onStopTrackingTouch(SeekBar b) {
                if (k == 0) p.iconDp = 40 + b.getProgress(); else p.gapDp = b.getProgress();
                apply(false);
            }
        });
        s.setLayoutParams(new LinearLayout.LayoutParams(p.px(160), WRAP));
        return s;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        return b;
    }

    void showSettings(boolean anim) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setOnClickListener(close);
        ScrollView sv = new ScrollView(this);
        sv.setClickable(true);
        sv.setBackgroundColor(bg());
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(p.px(20), p.px(16), p.px(20), p.px(16));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = tv("Settings", 22, fg());
        t.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1));
        TextView dn = tv("Done", 16, 0xFF2F7CF6);
        dn.setPadding(p.px(8), p.px(8), 0, p.px(8));
        dn.setOnClickListener(close);
        head.addView(dn);
        col.addView(head);

        final TextView val = tv("" + p.cols, 16, fg());
        val.setGravity(Gravity.CENTER);
        Button minus = btn("\u2212"), plus = btn("+");
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (p.cols > 4) { p.cols--; val.setText("" + p.cols); apply(false); } }
        });
        plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (p.cols < 8) { p.cols++; val.setText("" + p.cols); apply(false); } }
        });
        LinearLayout step = new LinearLayout(this);
        step.setGravity(Gravity.CENTER_VERTICAL);
        step.addView(minus);
        step.addView(val, new LinearLayout.LayoutParams(p.px(28), WRAP));
        step.addView(plus);

        col.addView(row("Columns (landscape)", step));
        col.addView(row("Icon size", sb(40, p.iconDp - 40, 0)));
        col.addView(row("Grid spacing", sb(24, p.gapDp, 1)));
        col.addView(row("Icon labels", sw(p.labels, 0)));
        col.addView(row("Dock", sw(p.dock, 1)));
        col.addView(row("Clock", sw(p.clock, 2)));
        col.addView(row("Dark appearance", sw(p.dark, 3)));
        col.addView(row("Animations", sw(p.anim, 4)));

        Button wp = btn("Change wallpaper");
        wp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeOverlay(false);
                try { startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper")); }
                catch (Exception e) { Toast.makeText(LauncherActivity.this, "No wallpaper picker found", Toast.LENGTH_SHORT).show(); }
            }
        });
        Button reset = btn("Reset home layout");
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                st.defaults(LauncherActivity.this, repo);
                persist();
                home.rebuild();
                rebuildDock();
            }
        });
        col.addView(wp);
        col.addView(reset);

        sv.addView(col);
        wrap.addView(sv, new FrameLayout.LayoutParams(p.px(340), MATCH, Gravity.RIGHT));
        root.addView(wrap, new FrameLayout.LayoutParams(MATCH, MATCH));
        if (anim && p.anim) {
            sv.setTranslationX(p.px(340));
            sv.animate().translationX(0).setDuration(220).setInterpolator(new DecelerateInterpolator());
        }
        overlay = wrap;
        kind = 3;
    }
}
