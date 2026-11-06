package com.delaybrain.glasshouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

// How a photo fills the screen when its shape isn't the screen's (a 4:3 or upright photo on a 16:9 TV):
// filled edge to edge with what sticks out cut off at the bottom and top (TOP keeps the top, BOTTOM the
// bottom, MIDDLE both halves), or WHOLE: all of it, on a soft, dark copy of itself that fills the rest.
// Picked separately for the wallpaper and the screensaver; drawn once into a screen-sized picture, so
// showing it costs nothing more than any other picture.
final class PhotoFit {
    static final String TOP = "top", MIDDLE = "middle", BOTTOM = "bottom", WHOLE = "whole";
    static final String[] ALL = {TOP, MIDDLE, BOTTOM, WHOLE};
    static final String[] NAMES = {"Top", "Middle", "Bottom", "Whole photo"};
    static final String WALLPAPER = "wallpaperFit", SAVER = "saverFit"; // where each choice is saved

    private PhotoFit() {}

    static String get(Context c, String key) {
        String s = c.getSharedPreferences("config", 0).getString(key, MIDDLE);
        for (String a : ALL) if (a.equals(s)) return a;
        return MIDDLE;
    }

    static void set(Context c, String key, String fit) {
        c.getSharedPreferences("config", 0).edit().putString(key, fit).apply();
    }

    // The photo at w x h as fit says; pic is recycled.
    static Bitmap compose(Bitmap pic, int w, int h, String fit) {
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint smooth = new Paint(Paint.FILTER_BITMAP_FLAG);
        float pw = pic.getWidth(), ph = pic.getHeight();
        if (WHOLE.equals(fit)) {
            // The backdrop: the photo shrunk to a few dozen pixels and stretched back over the screen,
            // which blurs it, then darkened so the photo stands out.
            Bitmap tiny = Bitmap.createScaledBitmap(pic, Math.max(1, Math.round(32 * pw / Math.max(pw, ph))),
                    Math.max(1, Math.round(32 * ph / Math.max(pw, ph))), true);
            float cover = Math.max(w / (float) tiny.getWidth(), h / (float) tiny.getHeight());
            float tw = tiny.getWidth() * cover, th = tiny.getHeight() * cover;
            c.drawBitmap(tiny, null, new RectF((w - tw) / 2, (h - th) / 2, (w + tw) / 2, (h + th) / 2), smooth);
            tiny.recycle();
            c.drawColor(0x8C000000);
            float s = Math.min(w / pw, h / ph);
            float dw = pw * s, dh = ph * s;
            c.drawBitmap(pic, null, new RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), smooth);
        } else {
            float s = Math.max(w / pw, h / ph);
            float dw = pw * s, dh = ph * s;
            float x = (w - dw) / 2; // a photo wider than the screen is always cut evenly at the sides
            float y = TOP.equals(fit) ? 0 : BOTTOM.equals(fit) ? h - dh : (h - dh) / 2;
            c.drawBitmap(pic, null, new RectF(x, y, x + dw, y + dh), smooth);
        }
        pic.recycle();
        return out;
    }
}
