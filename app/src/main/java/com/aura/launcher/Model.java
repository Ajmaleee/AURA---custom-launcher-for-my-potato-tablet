package com.aura.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/** An installed launchable app (label only; icons are loaded lazily by IconCache). */
class App { String comp, label; }

/** A home-screen entry: an app (comp != null) or a folder (kids != null). */
class Item {
    String comp, label, name;
    ArrayList<String> kids;
    boolean isFolder() { return kids != null; }
    static Item app(App a) { Item i = new Item(); i.comp = a.comp; i.label = a.label; return i; }
}

/** The installed-app list. Queried once, and again only when a package changes. */
class Repo {
    final ArrayList<App> apps = new ArrayList<App>();
    final HashMap<String, App> map = new HashMap<String, App>();

    void load(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        apps.clear();
        map.clear();
        for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
            if (r.activityInfo.packageName.equals(c.getPackageName())) continue;
            App a = new App();
            a.comp = new ComponentName(r.activityInfo.packageName, r.activityInfo.name).flattenToString();
            a.label = String.valueOf(r.loadLabel(pm));
            apps.add(a);
            map.put(a.comp, a);
        }
        Collections.sort(apps, new Comparator<App>() {
            public int compare(App x, App y) { return x.label.compareToIgnoreCase(y.label); }
        });
    }

    App firstOf(String pkg) {
        for (App a : apps) if (a.comp.startsWith(pkg + "/")) return a;
        return null;
    }
}

/** Home layout (flat list, paginated at layout time) and dock, persisted as JSON. */
class Store {
    final ArrayList<Item> home = new ArrayList<Item>();
    final ArrayList<String> dock = new ArrayList<String>();

    void load(Context c, Prefs p, Repo r) {
        if (!p.sp.contains("home")) { defaults(c, r); save(p); return; }
        try {
            JSONArray a = new JSONArray(p.sp.getString("home", "[]"));
            for (int i = 0; i < a.length(); i++) {
                Object o = a.get(i);
                if (o instanceof JSONObject) {
                    JSONObject j = (JSONObject) o;
                    Item f = new Item();
                    f.name = j.optString("n", "Folder");
                    f.kids = new ArrayList<String>();
                    JSONArray k = j.getJSONArray("k");
                    for (int x = 0; x < k.length(); x++) if (r.map.containsKey(k.getString(x))) f.kids.add(k.getString(x));
                    if (!f.kids.isEmpty()) home.add(f);
                } else {
                    App ap = r.map.get(String.valueOf(o));
                    if (ap != null) home.add(Item.app(ap));
                }
            }
            JSONArray d = new JSONArray(p.sp.getString("dock", "[]"));
            for (int i = 0; i < d.length(); i++) if (r.map.containsKey(d.getString(i))) dock.add(d.getString(i));
        } catch (JSONException e) { /* start with whatever loaded */ }
    }

    void save(Prefs p) {
        JSONArray a = new JSONArray();
        for (Item i : home) {
            if (i.isFolder()) {
                try {
                    JSONObject j = new JSONObject();
                    j.put("n", i.name);
                    j.put("k", new JSONArray(i.kids));
                    a.put(j);
                } catch (JSONException e) { }
            } else a.put(i.comp);
        }
        p.sp.edit().putString("home", a.toString()).putString("dock", new JSONArray(dock).toString()).apply();
    }

    /** Drop uninstalled apps and refresh labels. */
    void prune(Repo r) {
        for (Iterator<Item> it = home.iterator(); it.hasNext(); ) {
            Item i = it.next();
            if (i.isFolder()) { i.kids.retainAll(r.map.keySet()); if (i.kids.isEmpty()) it.remove(); }
            else if (!r.map.containsKey(i.comp)) it.remove();
            else i.label = r.map.get(i.comp).label;
        }
        dock.retainAll(r.map.keySet());
    }

    void defaults(Context c, Repo r) {
        home.clear();
        dock.clear();
        PackageManager pm = c.getPackageManager();
        Intent[] q = {
            new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")),
            new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")),
            new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")),
            new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent i : q) {
            ResolveInfo ri = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri == null || "android".equals(ri.activityInfo.packageName)) continue;
            App a = r.map.get(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name).flattenToString());
            if (a == null) a = r.firstOf(ri.activityInfo.packageName);
            if (a != null && !dock.contains(a.comp)) dock.add(a.comp);
        }
        for (App a : r.apps) if (home.size() < 24 && !dock.contains(a.comp)) home.add(Item.app(a));
        while (dock.size() < 4 && !home.isEmpty()) dock.add(home.remove(0).comp);
    }
}
