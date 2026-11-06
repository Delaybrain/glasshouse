package com.ansub.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
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
// With "Your photos" picked (Settings > Screensaver) it shows photos instead: from a USB stick or the
// TV, found through Android's media index, each loaded at screen size on a background thread, so at
// most two are in memory. Photos sent over Wi-Fi (PhotoDrop) are this app's own and need no permission.
// No photos: the colours as before. This view is the whole picture: Screensaver shows it while the TV
// is idle, SaverPreview when "Show now" is picked.
final class SaverView extends FrameLayout {
    private static final long SLIDE_MS = 14_000, FADE_MS = 2_500;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Drawable> washes = new ArrayList<>();
    private final List<android.net.Uri> photos = new ArrayList<>();
    private final java.util.concurrent.ExecutorService loader = java.util.concurrent.Executors.newSingleThreadExecutor();
    private ImageView front, back;
    private LinearLayout clock;
    private View shade; // darkens the bottom left for the clock: stays above each new picture
    private int next;
    private final float scale;

    SaverView(Context c) {
        super(c);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        setBackgroundColor(0xFF0E0F13);
        FrameLayout root = this;
        back = wash();
        front = wash();
        root.addView(back, new FrameLayout.LayoutParams(-1, -1));
        root.addView(front, new FrameLayout.LayoutParams(-1, -1));
        // Darker towards the bottom left, where the clock is.
        shade = new View(getContext());
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TR_BL,
                new int[] {0x00000000, 0x33000000, 0x99000000}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        clock = new LinearLayout(getContext());
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
    }

    // Starts the slides; stop() when the view goes.
    void start() {
        if (photos(getContext())) findPhotos(getContext(), photos, 300);
        if (!photos.isEmpty()) {
            java.util.Collections.shuffle(photos);
            front.setScaleType(ImageView.ScaleType.CENTER_CROP);
            back.setScaleType(ImageView.ScaleType.CENTER_CROP);
            showPhoto(); // the first one fades in as soon as it's loaded
        } else {
            loadWashes();
            if (!washes.isEmpty()) {
                front.setImageDrawable(washes.get(0));
                drift(front);
                next = 1;
            }
        }
        handler.postDelayed(slide, SLIDE_MS);
        handler.postDelayed(nudge, 5 * 60_000);
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
        loader.shutdownNow();
    }

    // The next app's colours fade in over the current ones.
    private final Runnable slide = new Runnable() {
        @Override public void run() {
            if (!photos.isEmpty()) {
                showPhoto();
            } else if (washes.size() > 1) {
                ImageView old = front;
                front = back;
                back = old;
                front.setImageDrawable(washes.get(next++ % washes.size()));
                front.bringToFront();
                shade.bringToFront();
                clock.bringToFront();
                front.setAlpha(0f);
                front.animate().alpha(1f).setDuration(FADE_MS).start();
                drift(front);
            }
            handler.postDelayed(this, SLIDE_MS);
        }
    };

    // Loads the next photo off the main thread, then cross-fades it in over the one showing.
    private void showPhoto() {
        final android.net.Uri uri = photos.get(next++ % photos.size());
        final int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
        loader.execute(() -> {
            android.graphics.Bitmap b;
            try {
                b = decode(uri, w, h);
            } catch (Exception | OutOfMemoryError e) {
                return; // gone, or not a picture after all: the next slide tries the next one
            }
            handler.post(() -> {
                ImageView old = front;
                front = back;
                back = old;
                front.setImageDrawable(new android.graphics.drawable.BitmapDrawable(getResources(), b));
                front.bringToFront();
                shade.bringToFront();
                clock.bringToFront();
                front.setAlpha(0f);
                front.animate().alpha(1f).setDuration(FADE_MS)
                        .withEndAction(() -> back.setImageDrawable(null)).start(); // only two in memory
                driftPhoto(front);
            });
        });
    }

    // The photo itself (not Android's small cached thumbnail, which looked blurry on the TV), scaled down
    // while decoding to just cover the screen: sharp, and no more memory than a screenful. ImageDecoder
    // also turns it the right way up and reads HEIC from iPhones.
    private android.graphics.Bitmap decode(android.net.Uri uri, final int w, final int h) throws java.io.IOException {
        android.graphics.ImageDecoder.Source src =
                android.graphics.ImageDecoder.createSource(getContext().getContentResolver(), uri);
        return android.graphics.ImageDecoder.decodeBitmap(src, (dec, info, s) -> {
            int iw = info.getSize().getWidth(), ih = info.getSize().getHeight();
            float scale = Math.max((float) w / iw, (float) h / ih);
            if (scale < 1f) dec.setTargetSize(Math.max(1, Math.round(iw * scale)), Math.max(1, Math.round(ih * scale)));
        });
    }

    // Photos move less than the washes: a slow Ken Burns zoom, so they stay sharp.
    private void driftPhoto(ImageView v) {
        boolean left = Math.random() < 0.5;
        v.setScaleX(1.08f);
        v.setScaleY(1.08f);
        v.setTranslationX(left ? px(-30) : px(30));
        v.animate().scaleX(1f).scaleY(1f).translationX(0)
                .setDuration(SLIDE_MS + FADE_MS).setInterpolator(new LinearInterpolator()).start();
    }

    // Whether "Your photos" is picked. Without the photo permission Android still shows this app its own
    // (the ones sent over Wi-Fi), so findPhotos decides what there is.
    static boolean photos(Context c) {
        return "photos".equals(c.getSharedPreferences("config", 0).getString("saver", null));
    }

    static int countPhotos(Context c) {
        List<android.net.Uri> found = new ArrayList<>();
        findPhotos(c, found, 1);
        return found.size();
    }

    // Photos on every storage Android indexes (the TV and USB sticks), newest first, up to max.
    static void findPhotos(Context c, List<android.net.Uri> out, int max) {
        android.net.Uri all = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (android.database.Cursor cur = c.getContentResolver().query(all,
                new String[] {android.provider.MediaStore.Images.Media._ID}, null, null,
                android.provider.MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            while (cur != null && cur.moveToNext() && out.size() < max)
                out.add(android.content.ContentUris.withAppendedId(all, cur.getLong(0)));
        } catch (Exception ignored) {} // no permission after all: no photos
    }

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
        PackageManager pm = getContext().getPackageManager();
        String saved = getContext().getSharedPreferences("config", 0).getString("apps", null);
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
        ImageView v = new ImageView(getContext());
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        return v;
    }

    private TextClock text(String f24, String f12, int size, int color, Typeface tf) {
        TextClock c = new TextClock(getContext());
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
