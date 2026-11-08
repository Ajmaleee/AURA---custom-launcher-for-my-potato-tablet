package com.aura.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import java.util.Calendar;

/** Procedurally drawn iOS-style icons (no image assets). Everything is drawn in a 100x100 space. */
class Ios {

    /** Pick an icon style from the component name and app label, or null for the default plate. */
    static String kind(String comp, String label) {
        String s = (comp + " " + label).toLowerCase();
        String l = label.toLowerCase().trim();
        if (s.contains("recorder")) return "memo";
        if (s.contains("voice")) return "mic";
        if (s.contains("gmail") || s.contains("android.gm")) return "gmail";
        if ((s.contains("google") && s.contains("setting")) || s.contains("gms")) return "gset";
        if (s.contains("vending") || s.contains("play store") || s.contains("playstore")) return "store";
        if (s.contains("maps")) return "maps";
        if (s.contains("quicksearchbox") || l.equals("google")) return "search";
        if (s.contains("download")) return "dl";
        if (s.contains("calendar")) return "cal";
        if (s.contains("clock")) return "clock";
        if (s.contains("calc")) return "calc";
        if (s.contains("camera")) return "cam";
        if (s.contains("music")) return "music";
        if (s.contains("contacts") || s.contains("people")) return "people";
        if (s.contains("mail")) return "mail";
        if (s.contains("moovie") || s.contains("movie")) return "movie";
        if (s.contains("uhd") || s.contains("player") || s.contains("video")) return "video";
        if (s.contains("file") || s.contains("manager") || s.contains("documentsui")) return "files";
        if (s.contains("browser") || s.contains("chrome")) return "web";
        if (s.contains("setting")) return "set";
        return null;
    }

    static Bitmap render(String k, int px) {
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.scale(px / 100f, px / 100f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        draw(c, p, k);
        // clip to an iOS-style rounded square with smooth edges
        Path m = new Path();
        m.addRoundRect(new RectF(0, 0, 100, 100), 22.5f, 22.5f, Path.Direction.CW);
        m.setFillType(Path.FillType.INVERSE_WINDING);
        p.reset();
        p.setAntiAlias(true);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        c.drawPath(m, p);
        return b;
    }

    // ---------------------------------------------------------------- helpers

    static void bg(Canvas c, Paint p, int top, int bot) {
        p.reset();
        p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, 0, 0, 100, top, bot, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 100, 100, p);
        p.setShader(null);
    }

    static void fill(Paint p, int color) {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
    }

    static void line(Paint p, int color, float w) {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(color);
        p.setStrokeWidth(w);
    }

    // ------------------------------------------------------------------ icons

    static void draw(Canvas c, Paint p, String k) {
        switch (k) {
            case "cal": cal(c, p); break;
            case "clock": clock(c, p); break;
            case "calc": calc(c, p); break;
            case "cam": cam(c, p); break;
            case "music": music(c, p); break;
            case "people": people(c, p); break;
            case "mail": mail(c, p, 0xFF5AC8FA, 0xFF0A74F0, 0xFF0A74F0); break;
            case "gmail": mail(c, p, 0xFFFF7A6E, 0xFFD93025, 0xFFD93025); break;
            case "files": files(c, p); break;
            case "web": web(c, p); break;
            case "set": gear(c, p, 0xFFA0A0A6, 0xFF5E5E64, 0xFF7F7F85); break;
            case "gset": gear(c, p, 0xFF7A8CF2, 0xFF4558D6, 0xFF5E70E0); break;
            case "maps": maps(c, p); break;
            case "store": store(c, p); break;
            case "search": search(c, p); break;
            case "dl": download(c, p); break;
            case "mic": mic(c, p); break;
            case "memo": memo(c, p); break;
            case "video": video(c, p); break;
            case "movie": movie(c, p); break;
            default: bg(c, p, 0xFFE5E5EA, 0xFFAEAEB4);
        }
    }

    static void cal(Canvas c, Paint p) {
        bg(c, p, 0xFFFFFFFF, 0xFFF4F4F6);
        Calendar t = Calendar.getInstance();
        String[] d = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        fill(p, 0xFFFF3B30);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(15);
        c.drawText(d[t.get(Calendar.DAY_OF_WEEK) - 1], 50, 29, p);
        fill(p, 0xFF1C1C1E);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        p.setTextSize(58);
        c.drawText("" + t.get(Calendar.DAY_OF_MONTH), 50, 84, p);
    }

    static void clock(Canvas c, Paint p) {
        bg(c, p, 0xFF2C2C2E, 0xFF000000);
        fill(p, 0xFFFFFFFF);
        c.drawCircle(50, 50, 41, p);
        line(p, 0xFF1C1C1E, 2.2f);
        for (int i = 0; i < 12; i++) {
            c.save();
            c.rotate(i * 30, 50, 50);
            c.drawLine(50, 13, 50, i % 3 == 0 ? 21 : 18, p);
            c.restore();
        }
        line(p, 0xFF1C1C1E, 4.2f);
        c.save(); c.rotate(-56, 50, 50); c.drawLine(50, 50, 50, 29, p); c.restore();
        c.save(); c.rotate(54, 50, 50); c.drawLine(50, 50, 50, 20, p); c.restore();
        line(p, 0xFFFF9500, 1.6f);
        c.save(); c.rotate(150, 50, 50); c.drawLine(50, 58, 50, 17, p); c.restore();
        fill(p, 0xFFFF9500);
        c.drawCircle(50, 50, 3.2f, p);
        fill(p, 0xFF1C1C1E);
        c.drawCircle(50, 50, 1.2f, p);
    }

    static void calc(Canvas c, Paint p) {
        bg(c, p, 0xFF3A3A3C, 0xFF1C1C1E);
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 4; i++) {
                int col = i == 3 ? 0xFFFF9F0A : (j == 0 ? 0xFFA5A5A5 : 0xFF4A4A4D);
                fill(p, col);
                c.drawCircle(21.5f + i * 19, 25f + j * 19, 8.5f, p);
            }
        }
    }

    static void cam(Canvas c, Paint p) {
        bg(c, p, 0xFFD1D1D6, 0xFF8E8E93);
        fill(p, 0xFF1C1C1E);
        c.drawRoundRect(new RectF(16, 32, 84, 76), 9, 9, p);
        c.drawRoundRect(new RectF(34, 25, 56, 38), 4, 4, p);
        fill(p, 0xFF6E6E73);
        c.drawCircle(50, 54, 17, p);
        fill(p, 0xFF0A0A0C);
        c.drawCircle(50, 54, 13, p);
        fill(p, 0xFF2C3E6B);
        c.drawCircle(50, 54, 8, p);
        fill(p, 0x66FFFFFF);
        c.drawCircle(46, 50, 3, p);
        fill(p, 0xFFFF9F0A);
        c.drawCircle(75, 41, 2.6f, p);
    }

    static void music(Canvas c, Paint p) {
        bg(c, p, 0xFFFB5C74, 0xFFFA233B);
        fill(p, 0xFFFFFFFF);
        c.save();
        c.rotate(-20, 40, 68);
        c.drawOval(new RectF(28, 60, 52, 77), p);
        c.restore();
        c.drawRect(49, 28, 55, 69, p);
        Path f = new Path();
        f.moveTo(55, 28);
        f.cubicTo(62, 36, 74, 36, 74, 52);
        f.cubicTo(70, 45, 62, 44, 55, 43);
        f.close();
        c.drawPath(f, p);
    }

    static void people(Canvas c, Paint p) {
        bg(c, p, 0xFFE5E5EA, 0xFFAEAEB4);
        fill(p, 0xFF636369);
        c.drawCircle(50, 39, 14, p);
        c.drawRoundRect(new RectF(24, 59, 76, 118), 26, 26, p);
    }

    static void mail(Canvas c, Paint p, int top, int bot, int flap) {
        bg(c, p, top, bot);
        fill(p, 0xFFFFFFFF);
        c.drawRoundRect(new RectF(18, 30, 82, 72), 6, 6, p);
        line(p, flap, 3.6f);
        Path f = new Path();
        f.moveTo(21, 35);
        f.lineTo(50, 57);
        f.lineTo(79, 35);
        c.drawPath(f, p);
    }

    static void files(Canvas c, Paint p) {
        bg(c, p, 0xFFFFFFFF, 0xFFEDEDF2);
        fill(p, 0xFF1B6EF0);
        c.drawRoundRect(new RectF(19, 27, 50, 42), 4, 4, p);
        c.drawRoundRect(new RectF(19, 33, 81, 70), 5, 5, p);
        p.setShader(new LinearGradient(0, 41, 0, 76, 0xFF58B6FF, 0xFF1F8CFA, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(19, 41, 81, 76), 5, 5, p);
        p.setShader(null);
    }

    static void web(Canvas c, Paint p) {
        bg(c, p, 0xFFFFFFFF, 0xFFE3EAF3);
        p.reset();
        p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, 12, 0, 88, 0xFF5AC8FA, 0xFF0A64FF, Shader.TileMode.CLAMP));
        c.drawCircle(50, 50, 38, p);
        line(p, 0xFFFFFFFF, 1.6f);
        for (int i = 0; i < 12; i++) {
            c.save();
            c.rotate(i * 30, 50, 50);
            c.drawLine(50, 15, 50, i % 3 == 0 ? 21 : 18, p);
            c.restore();
        }
        c.save();
        c.rotate(45, 50, 50);
        Path up = new Path();
        up.moveTo(50, 22); up.lineTo(58, 50); up.lineTo(42, 50); up.close();
        fill(p, 0xFFFF3B30);
        c.drawPath(up, p);
        Path dn = new Path();
        dn.moveTo(50, 78); dn.lineTo(58, 50); dn.lineTo(42, 50); dn.close();
        fill(p, 0xFFFFFFFF);
        c.drawPath(dn, p);
        c.restore();
    }

    static void gear(Canvas c, Paint p, int top, int bot, int hole) {
        bg(c, p, top, bot);
        fill(p, 0xFFDADADF);
        for (int i = 0; i < 8; i++) {
            c.save();
            c.rotate(i * 45, 50, 50);
            c.drawRoundRect(new RectF(43.5f, 15, 56.5f, 32), 2.5f, 2.5f, p);
            c.restore();
        }
        c.drawCircle(50, 50, 25, p);
        fill(p, hole);
        c.drawCircle(50, 50, 11, p);
    }

    static void maps(Canvas c, Paint p) {
        bg(c, p, 0xFFF1F5E9, 0xFFE4EDD9);
        fill(p, 0xFFBDE3A4);
        c.drawCircle(8, 92, 38, p);
        line(p, 0xFFFFD863, 13f);
        c.drawLine(-5, 76, 105, 34, p);
        line(p, 0xFFFFFFFF, 7f);
        c.drawLine(24, 108, 66, -8, p);
        fill(p, 0xFFFF3B30);
        c.drawCircle(64, 40, 15, p);
        Path t = new Path();
        t.moveTo(52, 49); t.lineTo(76, 49); t.lineTo(64, 72); t.close();
        c.drawPath(t, p);
        fill(p, 0xFFFFFFFF);
        c.drawCircle(64, 40, 5.5f, p);
    }

    static void store(Canvas c, Paint p) {
        bg(c, p, 0xFF1FC8FA, 0xFF1A6EF7);
        line(p, 0xFFFFFFFF, 7.5f);
        c.drawLine(50, 24, 33, 66, p);
        c.drawLine(50, 24, 67, 66, p);
        c.drawLine(40, 52, 60, 52, p);
        c.drawLine(27, 76, 73, 76, p);
    }

    static void search(Canvas c, Paint p) {
        bg(c, p, 0xFFFFFFFF, 0xFFE9EEF8);
        line(p, 0xFF2F7CF6, 8f);
        c.drawCircle(44, 44, 20, p);
        c.drawLine(59, 59, 76, 76, p);
    }

    static void download(Canvas c, Paint p) {
        bg(c, p, 0xFF4CD964, 0xFF1FA84A);
        line(p, 0xFFFFFFFF, 8.5f);
        c.drawLine(50, 22, 50, 56, p);
        Path a = new Path();
        a.moveTo(34, 42); a.lineTo(50, 58); a.lineTo(66, 42);
        c.drawPath(a, p);
        c.drawLine(28, 76, 72, 76, p);
    }

    static void mic(Canvas c, Paint p) {
        bg(c, p, 0xFF6C6CF2, 0xFF2E2BA8);
        fill(p, 0xFFFFFFFF);
        c.drawRoundRect(new RectF(40, 22, 60, 56), 10, 10, p);
        line(p, 0xFFFFFFFF, 5f);
        c.drawArc(new RectF(31, 34, 69, 70), 0, 180, false, p);
        c.drawLine(50, 70, 50, 80, p);
        c.drawLine(40, 80, 60, 80, p);
    }

    static void memo(Canvas c, Paint p) {
        bg(c, p, 0xFF2C2C2E, 0xFF000000);
        float[] h = {12, 26, 42, 60, 38, 22, 14};
        for (int i = 0; i < h.length; i++) {
            line(p, i == 3 ? 0xFFFF3B30 : 0xFFBFBFC4, 4.6f);
            float x = 22 + i * 9.4f;
            c.drawLine(x, 50 - h[i] / 2, x, 50 + h[i] / 2, p);
        }
    }

    static void video(Canvas c, Paint p) {
        bg(c, p, 0xFF3A3A3C, 0xFF000000);
        fill(p, 0xFFFFFFFF);
        p.setPathEffect(new CornerPathEffect(5));
        Path t = new Path();
        t.moveTo(39, 30); t.lineTo(72, 50); t.lineTo(39, 70); t.close();
        c.drawPath(t, p);
    }

    static void movie(Canvas c, Paint p) {
        bg(c, p, 0xFF8F6BFF, 0xFF4A2FD0);
        fill(p, 0xFFFFFFFF);
        p.setPathEffect(new CornerPathEffect(2));
        Path s = new Path();
        for (int i = 0; i < 10; i++) {
            float r = i % 2 == 0 ? 27 : 11.5f;
            double a = Math.toRadians(-90 + i * 36);
            float x = 50 + (float) (r * Math.cos(a)), y = 52 + (float) (r * Math.sin(a));
            if (i == 0) s.moveTo(x, y); else s.lineTo(x, y);
        }
        s.close();
        c.drawPath(s, p);
    }
}