package com.ansub.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

// The remote's Settings button, like Apple TV's Control Center: a glass panel slides in at the right,
// over whatever is playing, with the screens people actually need. On Google TV that button opened
// Google TV's own panel, which went with Google TV's home screen; HomeGuard sends it here instead.
// Back, or OK on a row, closes it.
public class ControlCenter extends Activity {
    private static final String BT_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    private float scale = 1f;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
        Typeface semibold = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Style.DIM);
        root.setOnClickListener(v -> finish());

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(px(Style.PANEL_PAD), px(Style.PANEL_PAD + 12), px(Style.PANEL_PAD), px(Style.PANEL_PAD));
        // Over the home screen: the same glass as the menus, over its blurred copy. Over another app its
        // picture can't be copied, so the glass is tinted more instead.
        int sw = getResources().getDisplayMetrics().widthPixels, sh = getResources().getDisplayMetrics().heightPixels;
        android.graphics.drawable.Drawable glass = MainActivity.glassOverHome(new android.graphics.Rect(
                sw - px(Style.PANEL_W + Style.PANEL_MARGIN), px(Style.PANEL_MARGIN), sw - px(Style.PANEL_MARGIN), sh - px(Style.PANEL_MARGIN)));
        panel.setBackground(glass != null ? glass : new LiquidGlass(px(Style.RADIUS_PANEL), Math.max(1, px(Style.STROKE)),
                Style.LIQUID_SOLO, null, null));

        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("H:mm");
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(Style.HEADLINE));
        time.setTextColor(Style.INK);
        time.setTypeface(semibold);
        time.setPadding(px(20), 0, 0, 0);
        panel.addView(time);
        TextView head = new TextView(this);
        head.setText(Strings.t("Control Center"));
        head.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(Style.BODY));
        head.setTextColor(Style.INK_2);
        head.setTypeface(regular);
        head.setPadding(px(20), 0, 0, px(24));
        panel.addView(head);

        View first = null;
        for (final Row r : rows()) {
            View row = row(r, regular);
            if (first == null) first = row;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(Style.ROW_H));
            lp.topMargin = px(8);
            panel.addView(row, lp);
        }

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(px(Style.PANEL_W), -1, Gravity.END);
        pp.setMargins(0, px(Style.PANEL_MARGIN), px(Style.PANEL_MARGIN), px(Style.PANEL_MARGIN));
        root.addView(panel, pp);
        setContentView(root);

        // Slide in from the right.
        panel.setTranslationX(px(640));
        panel.animate().translationX(0).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        if (first != null) first.requestFocus();
    }

    private static final class Row {
        final String label; final int[] icon; final Runnable go;
        TextView text; // set by row(), so a row can change its own label (the sleep row)
        Row(String label, int[] icon, Runnable go) { this.label = label; this.icon = icon; this.go = go; }
    }

    // Turn the TV off in a while, whatever is playing (see HomeGuard.setOffTimer). OK steps through
    // these; with a timer running it goes on from the time left.
    private static final int[] OFF_MINUTES = {0, 30, 60, 90, 120};

    private String offLabel() {
        HomeGuard g = HomeGuard.running;
        int m = g == null ? 0 : g.offTimerMinutes();
        return Strings.t("TV off in") + ":  " + (m == 0 ? Strings.t("Off") : m + " " + Strings.t("min"));
    }

    private void nextOffTimer(Row r) {
        HomeGuard g = HomeGuard.running;
        if (g == null) return;
        int now = g.offTimerMinutes(), next = OFF_MINUTES[0];
        for (int m : OFF_MINUTES) if (m > now) { next = m; break; }
        g.setOffTimer(next);
        r.text.setText(offLabel());
    }

    // When the screensaver starts, or off. Its own settings page went with Google TV's screensaver, so
    // it's here. The wait is Settings.System's screen-off timeout (on a TV that starts the screensaver),
    // which needs WRITE_SETTINGS: adb shell appops set com.ansub.tvhome WRITE_SETTINGS allow
    private static final int[] SAVER_MINUTES = {0, 5, 10, 15, 30}; // 0: off

    private int saverMinutes() {
        if (Settings.Secure.getInt(getContentResolver(), "screensaver_enabled", 1) == 0) return 0;
        return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0) / 60000;
    }

    private String saverLabel() {
        int m = saverMinutes();
        return Strings.t("Screensaver after") + ":  " + (m == 0 ? Strings.t("Off") : m + " " + Strings.t("min"));
    }

    private void nextSaver(Row r) {
        int now = saverMinutes(), next = SAVER_MINUTES[0];
        for (int m : SAVER_MINUTES) if (m > now) { next = m; break; }
        try {
            Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", next == 0 ? 0 : 1);
            if (next > 0) Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, next * 60000);
            TimeoutGuard.remember(this, "screensaver_enabled", next == 0 ? 0 : 1); // HomeGuard keeps them so
            if (next > 0) TimeoutGuard.remember(this, Settings.System.SCREEN_OFF_TIMEOUT, next * 60000);
        } catch (SecurityException e) {
            return;
        }
        r.text.setText(saverLabel());
    }

    // How long the TV waits without a button press before it goes to sleep. OK steps through these and
    // the panel stays open. Only with: adb shell pm grant com.ansub.tvhome android.permission.WRITE_SECURE_SETTINGS
    private static final int[] SLEEP_MINUTES = {0, 15, 30, 60, 120}; // 0: never
    private static final String SLEEP = "sleep_timeout"; // Settings.Secure, in ms, -1 for never

    private int sleepMinutes() {
        int ms = Settings.Secure.getInt(getContentResolver(), SLEEP, -1);
        return ms <= 0 ? 0 : ms / 60000;
    }

    private String sleepLabel() {
        int m = sleepMinutes();
        return Strings.t("Sleep after") + ":  " + (m == 0 ? Strings.t("Off") : m + " " + Strings.t("min"));
    }

    private void nextSleep(Row r) {
        int now = sleepMinutes(), next = SLEEP_MINUTES[0];
        for (int m : SLEEP_MINUTES) if (m > now) { next = m; break; }
        try {
            Settings.Secure.putInt(getContentResolver(), SLEEP, next == 0 ? -1 : next * 60000);
            TimeoutGuard.remember(this, SLEEP, next == 0 ? -1 : next * 60000); // HomeGuard keeps it so
        } catch (SecurityException e) {
            return;
        }
        r.text.setText(sleepLabel());
    }

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        out.add(new Row("Bluetooth", LineIcon.BLUETOOTH, this::openBluetooth));
        out.add(new Row(Strings.t("Network"), LineIcon.WIFI, () -> start(new Intent(Settings.ACTION_WIFI_SETTINGS))));
        out.add(new Row(Strings.t("Picture"), LineIcon.PICTURE, () -> start(new Intent(Settings.ACTION_DISPLAY_SETTINGS))));
        out.add(new Row(Strings.t("Sound"), LineIcon.SOUND, () -> start(new Intent(Settings.ACTION_SOUND_SETTINGS))));
        if (HomeGuard.running != null) {
            final Row[] off = new Row[1];
            off[0] = new Row(offLabel(), LineIcon.POWER, () -> nextOffTimer(off[0]));
            out.add(off[0]);
        }
        if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
                && Settings.System.canWrite(this)) {
            final Row[] saver = new Row[1];
            saver[0] = new Row(saverLabel(), LineIcon.PICTURE, () -> nextSaver(saver[0]));
            out.add(saver[0]);
        }
        if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED) {
            final Row[] sleep = new Row[1];
            sleep[0] = new Row(sleepLabel(), LineIcon.CLOCK, () -> nextSleep(sleep[0]));
            out.add(sleep[0]);
        }
        out.add(new Row(Strings.t("All settings"), LineIcon.SETTINGS, () -> start(new Intent(Settings.ACTION_SETTINGS))));
        return out;
    }

    private View row(final Row r, Typeface tf) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(20), 0, px(20), 0);
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(Style.RADIUS_TILE));
        bg.setColor(Color.TRANSPARENT);
        row.setBackground(bg);
        final ImageView icon = new ImageView(this);
        icon.setImageDrawable(new LineIcon(r.icon));
        row.addView(icon, new LinearLayout.LayoutParams(px(40), px(40)));
        final TextView t = new TextView(this);
        t.setText(r.label);
        r.text = t;
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(Style.BODY));
        t.setTextColor(Style.INK);
        t.setTypeface(tf);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = px(24);
        row.addView(t, lp);
        row.setFocusable(true);
        // Focus turns the row white with dark text, like the home screen's glass tiles.
        row.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? Style.FOCUS : Color.TRANSPARENT);
            t.setTextColor(has ? Style.FOCUS_INK : Style.INK);
            if (has) icon.setColorFilter(new PorterDuffColorFilter(Style.FOCUS_INK, PorterDuff.Mode.SRC_IN));
            else icon.clearColorFilter();
        });
        row.setOnClickListener(v -> r.go.run());
        return row;
    }

    private void start(Intent i) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (Exception ignored) {}
        }
        finish();
    }

    // Android TV's only Bluetooth screen is its pairing screen, which needs "Nearby devices" on Android 12+.
    private void openBluetooth() {
        if (android.os.Build.VERSION.SDK_INT >= 31 && checkSelfPermission(BT_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {BT_CONNECT}, 1);
            return;
        }
        start(new Intent("com.google.android.intent.action.CONNECT_INPUT"));
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) openBluetooth();
        else start(new Intent(Settings.ACTION_SETTINGS));
    }

    private int px(int v) {
        return Math.round(v * scale);
    }
}
