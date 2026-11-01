package com.ansub.tvhome;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;

import java.util.ArrayList;
import java.util.List;

// The screensaver: a big clock and the date over the colours of the home screen's apps, a soft wash of
// one app's banner at a time that drifts slowly and cross-fades into the next every SLIDE_MS. Only
// small blurred bitmaps and two views move, so it costs next to nothing on a TV with little memory.
// The clock shifts a little every few minutes, so nothing sits in one place for hours.
public class Screensaver extends DreamService {
    private static final long SLIDE_MS = 14_000, FADE_MS = 2_500;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Drawable> washes = new ArrayList<>();
    private ImageView front, back;
    private LinearLayout clock;
    private int next;
    private float scale;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false); // any button wakes the TV and ends the screensaver
        setFullscreen(true);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0E0F13);
        back = wash();
        front = wash();
        root.addView(back, new FrameLayout.LayoutParams(-1, -1));
        root.addView(front, new FrameLayout.LayoutParams(-1, -1));
        // Darker towards the bottom left, where the clock is.
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TR_BL,
                new int[] {0x00000000, 0x33000000, 0x99000000}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        TextClock time = text("H:mm", "h:mm", px(200), Color.WHITE, light);
        time.setLetterSpacing(-0.03f);
        String day = Strings.datePattern();
        TextClock date = text(day, day, px(40), 0xCCFFFFFF, Typeface.create("sans-serif", Typeface.NORMAL));
        clock.addView(time);
        clock.addView(date);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        cp.leftMargin = px(104);
        cp.bottomMargin = px(96);
        root.addView(clock, cp);
        setContentView(root);

        loadWashes();
        if (!washes.isEmpty()) {
            front.setImageDrawable(washes.get(0));
            drift(front);
            next = 1;
        }
        handler.postDelayed(slide, SLIDE_MS);
        handler.postDelayed(nudge, 5 * 60_000);
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    // The next app's colours fade in over the current ones.
    private final Runnable slide = new Runnable() {
        @Override public void run() {
            if (washes.size() > 1) {
                ImageView old = front;
                front = back;
                back = old;
                front.setImageDrawable(washes.get(next++ % washes.size()));
                front.bringToFront();
                clock.bringToFront();
                front.setAlpha(0f);
                front.animate().alpha(1f).setDuration(FADE_MS).start();
                drift(front);
            }
            handler.postDelayed(this, SLIDE_MS);
        }
    };

    // Moves the clock a little, so it never stands still for hours.
    private final Runnable nudge = new Runnable() {
        @Override public void run() {
            clock.animate().translationX((float) (Math.random() * px(120)))
                    .translationY((float) (-Math.random() * px(80))).setDuration(4000).start();
            handler.postDelayed(this, 5 * 60_000);
        }
    };

    // A slow pan and zoom across the wash, the length of one slide plus the fade.
    private void drift(ImageView v) {
        boolean left = Math.random() < 0.5;
        v.setScaleX(1.25f);
        v.setScaleY(1.25f);
        v.setTranslationX(left ? px(-80) : px(80));
        v.animate().scaleX(1.1f).scaleY(1.1f).translationX(left ? px(80) : px(-80))
                .setDuration(SLIDE_MS + FADE_MS).setInterpolator(new LinearInterpolator()).start();
    }

    // One wash per app on the home screen's top row (the same apps, in the same order).
    private void loadWashes() {
        PackageManager pm = getPackageManager();
        String saved = getSharedPreferences("config", 0).getString("apps", null);
        String[] apps = saved != null ? saved.split("[,\\s]+") : Apps.LIST;
        for (String pkg : apps) {
            Intent li = MainActivity.launchIntent(pm, pkg);
            if (li == null) continue;
            Drawable art = null;
            try {
                art = pm.getActivityBanner(li);
                if (art == null) art = pm.getApplicationBanner(pkg);
                if (art == null) art = pm.getApplicationIcon(pkg);
            } catch (Exception ignored) {}
            if (art != null) washes.add(MainActivity.wash(getResources(), MainActivity.fit(getResources(), art, 96, 54), false));
        }
    }

    private ImageView wash() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        return v;
    }

    private TextClock text(String f24, String f12, int size, int color, Typeface tf) {
        TextClock c = new TextClock(this);
        c.setFormat24Hour(f24);
        c.setFormat12Hour(f12);
        c.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        c.setTextColor(color);
        c.setTypeface(tf);
        c.setIncludeFontPadding(false);
        return c;
    }

    private int px(int v) {
        return Math.round(v * scale);
    }
}
