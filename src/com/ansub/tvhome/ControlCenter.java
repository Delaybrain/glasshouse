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
        root.setBackgroundColor(0x59000000);
        root.setOnClickListener(v -> finish());

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(px(36), px(44), px(36), px(36));
        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(px(40));
        glass.setColor(0xF21C1C1E);
        glass.setStroke(Math.max(1, px(2)), 0x1FFFFFFF);
        panel.setBackground(glass);
        panel.setElevation(px(30));

        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("H:mm");
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(64));
        time.setTextColor(Color.WHITE);
        time.setTypeface(semibold);
        time.setPadding(px(20), 0, 0, 0);
        panel.addView(time);
        TextView head = new TextView(this);
        head.setText(Strings.t("Control Center"));
        head.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28));
        head.setTextColor(0xA6FFFFFF);
        head.setTypeface(regular);
        head.setPadding(px(20), 0, 0, px(24));
        panel.addView(head);

        View first = null;
        for (final Row r : rows()) {
            View row = row(r, regular);
            if (first == null) first = row;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(84));
            lp.topMargin = px(8);
            panel.addView(row, lp);
        }

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(px(600), -1, Gravity.END);
        pp.setMargins(0, px(32), px(32), px(32));
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
        } catch (SecurityException e) {
            return;
        }
        r.text.setText(sleepLabel());
    }

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        final Intent live = MainActivity.liveTvIntent(this);
        if (live != null) out.add(new Row(Strings.t("Live TV"), LineIcon.LIVE, () -> start(live)));
        out.add(new Row("Bluetooth", LineIcon.BLUETOOTH, this::openBluetooth));
        out.add(new Row(Strings.t("Network"), LineIcon.WIFI, () -> start(new Intent(Settings.ACTION_WIFI_SETTINGS))));
        out.add(new Row(Strings.t("Picture"), LineIcon.PICTURE, () -> start(new Intent(Settings.ACTION_DISPLAY_SETTINGS))));
        out.add(new Row(Strings.t("Sound"), LineIcon.SOUND, () -> start(new Intent(Settings.ACTION_SOUND_SETTINGS))));
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
        bg.setCornerRadius(px(22));
        bg.setColor(Color.TRANSPARENT);
        row.setBackground(bg);
        final ImageView icon = new ImageView(this);
        icon.setImageDrawable(new LineIcon(r.icon));
        row.addView(icon, new LinearLayout.LayoutParams(px(40), px(40)));
        final TextView t = new TextView(this);
        t.setText(r.label);
        r.text = t;
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(30));
        t.setTextColor(0xE6FFFFFF);
        t.setTypeface(tf);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = px(24);
        row.addView(t, lp);
        row.setFocusable(true);
        // Focus turns the row white with dark text, like the home screen's glass tiles.
        row.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0xF2FFFFFF : Color.TRANSPARENT);
            t.setTextColor(has ? 0xFF1C1C1E : 0xE6FFFFFF);
            if (has) icon.setColorFilter(new PorterDuffColorFilter(0xFF1C1C1E, PorterDuff.Mode.SRC_IN));
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
