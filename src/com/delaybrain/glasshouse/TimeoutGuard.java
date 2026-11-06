package com.delaybrain.glasshouse;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import java.io.File;
import java.io.FileWriter;
import java.util.function.Supplier;

// Keeps the waits picked in Control Center (screensaver after, sleep after) as picked. Something on this
// TV puts them back now and then (seen after restarts: the sleep wait back to "never", the screensaver
// wait back to two hours), so HomeGuard watches them: a change from anywhere else is undone at once and
// written down (see log), to find out what does it. A change made in the TV's own settings screens is
// the user's, so that one is kept as the new choice.
final class TimeoutGuard extends ContentObserver {
    private static final String PREFS = "config", WANT = "want.";
    private static final String TV_SETTINGS = "com.android.tv.settings";
    // namespace, name
    private static final String[][] KEYS = {{"system", Settings.System.SCREEN_OFF_TIMEOUT},
            {"secure", "sleep_timeout"}, {"secure", "screensaver_enabled"}};

    private final Context context;
    private final Supplier<String> front; // the app in front (HomeGuard knows)

    TimeoutGuard(Context c, Handler h, Supplier<String> front) {
        super(h);
        context = c;
        this.front = front;
    }

    // Called by Control Center when the user picks a wait.
    static void remember(Context c, String name, int value) {
        c.getSharedPreferences(PREFS, 0).edit().putInt(WANT + name, value).apply();
    }

    void start() {
        // The first time: the waits as they are now are the ones to keep (picked in Control Center before
        // there was a guard).
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
        if (!prefs.getBoolean(WANT + "seeded", false)) {
            for (String[] k : KEYS) {
                int now = get(k);
                if (now != Integer.MIN_VALUE) remember(context, k[1], now);
            }
            prefs.edit().putBoolean(WANT + "seeded", true).apply();
        }
        ContentResolver cr = context.getContentResolver();
        for (String[] k : KEYS) cr.registerContentObserver(uri(k), false, this);
        check(); // a reset during a restart happened before this was running
    }

    void stop() {
        context.getContentResolver().unregisterContentObserver(this);
    }

    @Override
    public void onChange(boolean self) {
        check();
    }

    private void check() {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
        for (String[] k : KEYS) {
            if (!prefs.contains(WANT + k[1])) continue; // never picked here: the TV's own value stands
            int want = prefs.getInt(WANT + k[1], 0), now = get(k);
            if (now == want) continue;
            if (TV_SETTINGS.equals(front.get())) { // changed by hand in the TV's settings: that's the choice now
                remember(context, k[1], now);
                log(k[1] + " " + want + " -> " + now + " (TV settings, kept)");
                continue;
            }
            try {
                if (k[0].equals("system")) Settings.System.putInt(context.getContentResolver(), k[1], want);
                else Settings.Secure.putInt(context.getContentResolver(), k[1], want);
                log(k[1] + " was " + now + ", put back to " + want + " (front: " + front.get() + ")");
            } catch (SecurityException e) {
                log(k[1] + " was " + now + ", can't put back: " + e.getMessage());
            }
        }
    }

    private int get(String[] k) {
        ContentResolver cr = context.getContentResolver();
        return k[0].equals("system") ? Settings.System.getInt(cr, k[1], Integer.MIN_VALUE)
                : Settings.Secure.getInt(cr, k[1], Integer.MIN_VALUE);
    }

    private static Uri uri(String[] k) {
        return k[0].equals("system") ? Settings.System.getUriFor(k[1]) : Settings.Secure.getUriFor(k[1]);
    }

    // One line per change, newest last; started over when it gets long. Read it with:
    // adb shell cat /sdcard/Android/data/com.delaybrain.glasshouse/files/timeouts.log
    private void log(String line) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) return;
        File f = new File(dir, "timeouts.log");
        boolean fresh = f.length() > 16 * 1024;
        try (FileWriter w = new FileWriter(f, !fresh)) {
            w.write(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT)
                    .format(new java.util.Date()) + "  " + line + "\n");
        } catch (Exception ignored) {}
    }
}
