package com.delaybrain.glasshouse;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

// An HDMI input full screen, in the home screen's look instead of the TV's own player (Settings > HDMI
// inputs > Open HDMI with Glasshouse). The picture is Android's TvView, the same as the live preview
// behind the home screen, so the TV still does the picture itself (its picture modes, HDR, game mode).
// On top, on liquid glass: a badge with the input and what's coming in (a few seconds after switching),
// a card when there's no signal (with the inputs that have a device on, and the TV turns off after a
// while), and a strip of all inputs to switch between.
//
// Buttons: Back goes one step back, as everywhere in Glasshouse: from the picture to the strip of inputs,
// from there home. For a device that speaks HDMI-CEC (a console, a player) the arrows and OK go to the
// device, as with the TV's player (whether it does anything with them is up to the device: a Switch
// doesn't); for any other input (a PC) they open the strip too. Home always goes home.
public class InputPlayer extends Activity {
    static final String INPUT = "input";       // the input to show, an extra
    static final String PREF = "hdmiPlayer";   // true: the home screen's HDMI pills open this
    static String lastShown;                   // the input shown last, so Home comes back on its pill

    private static final long NO_PICTURE_MS = 3000, BADGE_MS = 4500, STRIP_MS = 6000;
    private static final int STANDBY_MIN = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private float scale = 1f;
    private Typeface regular, semibold;
    private FrameLayout root;
    private TvView tv;
    private String current;
    private boolean picture;
    private View badge, card, strip;

    private final Runnable noSignal = this::showNoSignal;
    private final Runnable hideBadge = () -> fade(badge, false);
    private final Runnable hideStrip = this::closeStrip;
    private final Runnable standby = () -> {
        if (HomeGuard.running != null) HomeGuard.running.standby();
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        regular = MainActivity.font(false);
        semibold = MainActivity.font(true);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        tv = new TvView(this);
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setCallback(new TvView.TvInputCallback() {
            @Override public void onVideoAvailable(String id) {
                if (!id.equals(current)) return;
                picture = true;
                Inputs.picture(id, true);
                handler.removeCallbacks(noSignal);
                handler.removeCallbacks(standby);
                fade(card, false);
                if (strip == null || strip.getVisibility() != View.VISIBLE) tv.requestFocus();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                showBadge();
            }
            @Override public void onVideoUnavailable(String id, int reason) {
                if (!id.equals(current) || reason == TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING
                        || reason == TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING) return;
                picture = false;
                handler.removeCallbacks(noSignal);
                handler.postDelayed(noSignal, 800); // a picture that comes straight back isn't "no signal"
            }
            @Override public void onTracksChanged(String id, List<TvTrackInfo> tracks) {
                if (id.equals(current) && picture) showBadge(); // the resolution is often known only now
            }
            @Override public void onConnectionFailed(String id) { if (id.equals(current)) showNoSignal(); }
            @Override public void onDisconnected(String id) { if (id.equals(current)) showNoSignal(); }
        });
        // What a CEC device didn't take: arrows and OK open the strip, Back leaves.
        tv.setOnUnhandledInputEventListener(ev -> ev instanceof KeyEvent && ourKey((KeyEvent) ev));
        root.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        String id = getIntent().getStringExtra(INPUT);
        if (id == null) { finish(); return; }
        tune(id);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Left (Home, another app): free the input for the home screen's preview and the TV's own player.
        handler.removeCallbacksAndMessages(null);
        try { tv.reset(); } catch (Exception ignored) {}
        finish();
    }

    // Switch to an input: a clean session (like the preview), its name at once, "no signal" if no
    // picture comes within a few seconds.
    private void tune(String id) {
        current = id;
        lastShown = id;
        picture = false;
        handler.removeCallbacks(noSignal);
        handler.removeCallbacks(standby);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fade(card, false);
        try { tv.reset(); } catch (Exception ignored) {}
        try {
            tv.tune(id, TvContract.buildChannelUriForPassthroughInput(id));
        } catch (Exception e) {
            showNoSignal();
            return;
        }
        tv.requestFocus();
        showBadge();
        handler.postDelayed(noSignal, NO_PICTURE_MS);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        // Back: from the picture to the strip, from the strip (or the no-signal card) home.
        if (ev.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (ev.getAction() == KeyEvent.ACTION_UP) {
                if (overlayUp()) finish();
                else openStrip();
            }
            return true;
        }
        // Over the strip or the card, their buttons get the keys. On the picture, an input without a
        // CEC device has nothing to send them to: they're ours. With one they go to the device first.
        if (!overlayUp() && !Inputs.cecDevices(this).containsKey(current) && ourKey(ev)) return true;
        if (strip != null && strip.getVisibility() == View.VISIBLE) {
            handler.removeCallbacks(hideStrip);
            handler.postDelayed(hideStrip, STRIP_MS);
        }
        return super.dispatchKeyEvent(ev);
    }

    private boolean overlayUp() {
        return (strip != null && strip.getVisibility() == View.VISIBLE) || (card != null && card.getVisibility() == View.VISIBLE);
    }

    // Arrows, OK and Info show the strip (and the badge again). True when taken.
    private boolean ourKey(KeyEvent ev) {
        int code = ev.getKeyCode();
        boolean open = code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_INFO;
        if (!open) return false;
        if (ev.getAction() != KeyEvent.ACTION_UP) return true;
        showBadge();
        openStrip();
        return true;
    }

    // ---- The badge: "HDMI 2 · Nintendo Switch", and "1080p · 60 Hz" once the picture is there ----

    private void showBadge() {
        if (badge != null) root.removeView(badge);
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(px(32), px(20), px(36), px(22));
        TextView name = text(px(Style.BODY), Style.INK, semibold);
        name.setText(title(current));
        b.addView(name);
        String info = picture ? videoInfo() : null;
        if (info != null) {
            TextView what = text(px(Style.LABEL), Style.INK_2, regular);
            what.setText(info);
            what.setCompoundDrawablesRelative(dot(), null, null, null);
            what.setCompoundDrawablePadding(px(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = px(4);
            b.addView(what, lp);
        }
        b.setBackground(glass(Style.RADIUS_PANEL));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        lp.leftMargin = lp.topMargin = px(Style.PANEL_MARGIN);
        root.addView(b, lp);
        badge = b;
        b.setAlpha(0f);
        fade(b, true);
        handler.removeCallbacks(hideBadge);
        handler.postDelayed(hideBadge, BADGE_MS);
    }

    // What's coming in, like "1080p · 60 Hz", from the input's video track; null if the TV doesn't say.
    private String videoInfo() {
        try {
            List<TvTrackInfo> tracks = tv.getTracks(TvTrackInfo.TYPE_VIDEO);
            if (tracks == null || tracks.isEmpty()) return null;
            String selected = tv.getSelectedTrack(TvTrackInfo.TYPE_VIDEO);
            TvTrackInfo video = tracks.get(0);
            for (TvTrackInfo t : tracks) if (t.getId().equals(selected)) video = t;
            int h = video.getVideoHeight();
            if (h <= 0) return null;
            String size = h >= 2000 ? "4K" : h + "p";
            float fps = video.getVideoFrameRate();
            return fps > 0 ? size + "  ·  " + Math.round(fps) + " Hz" : size;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- No signal: a card in the middle, with the inputs that have a device on ----

    private void showNoSignal() {
        if (picture) return;
        Inputs.picture(current, false);
        handler.removeCallbacks(noSignal);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fade(badge, false);
        if (card != null) root.removeView(card);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(px(56), px(48), px(56), px(44));
        c.setBackground(glass(Style.RADIUS_PANEL));
        View icon = new View(this);
        LineIcon li = new LineIcon(LineIcon.HDMI);
        icon.setBackground(li);
        c.addView(icon, new LinearLayout.LayoutParams(px(64), px(64)));
        TextView head = text(px(Style.TITLE), Style.INK, semibold);
        head.setText(String.format(Strings.t("No signal on %s"), Inputs.label(this, current)));
        head.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
        hp.topMargin = px(20);
        c.addView(head, hp);
        TextView how = text(px(Style.BODY), Style.INK_2, regular);
        how.setText(Strings.t("Turn the device on, or check the cable."));
        how.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-2, -2);
        wp.topMargin = px(8);
        c.addView(how, wp);
        // Where else to go: inputs with a device on, then the home screen.
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Map<String, String> on = Inputs.cecDevices(this);
        View first = null;
        for (TvInputInfo in : Inputs.hdmi(this)) {
            if (in.getId().equals(current) || !(on.containsKey(in.getId()) || Inputs.pictureSeen(in.getId()))) continue;
            final String id = in.getId();
            View p = pill(title(id), true, () -> tune(id));
            if (first == null) first = p;
            row.addView(p, pillParams());
        }
        View home = pill(Strings.t("Home"), false, this::finish);
        if (first == null) first = home;
        row.addView(home, pillParams());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2);
        rp.topMargin = px(36);
        c.addView(row, rp);
        if (HomeGuard.running != null) {
            TextView note = text(px(Style.LABEL), Style.INK_3, regular);
            note.setText(String.format(Strings.t("Without a signal the TV turns off after %d minutes."), STANDBY_MIN));
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
            np.topMargin = px(28);
            c.addView(note, np);
            handler.removeCallbacks(standby);
            handler.postDelayed(standby, STANDBY_MIN * 60_000L);
        }
        root.addView(c, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        card = c;
        c.setAlpha(0f);
        fade(c, true);
        first.requestFocus();
    }

    // ---- The strip: every input, like the home screen's dock ----

    private void openStrip() {
        if (strip != null && strip.getVisibility() == View.VISIBLE) return;
        if (strip != null) root.removeView(strip);
        LinearLayout s = new LinearLayout(this);
        s.setGravity(Gravity.CENTER);
        s.setPadding(px(24), px(24), px(24), px(24));
        s.setBackground(glass(Style.RADIUS_PANEL));
        View now = null;
        for (TvInputInfo in : Inputs.hdmi(this)) {
            final String id = in.getId();
            boolean on = Inputs.cecDevices(this).containsKey(id) || Inputs.pictureSeen(id) || (id.equals(current) && picture);
            View p = pill(title(id), on, () -> {
                closeStrip();
                if (!id.equals(current)) tune(id);
            });
            if (id.equals(current)) now = p;
            s.addView(p, pillParams());
        }
        s.addView(pill(Strings.t("Home"), false, this::finish), pillParams());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = px(Style.PANEL_MARGIN);
        root.addView(s, lp);
        strip = s;
        s.setAlpha(0f);
        fade(s, true);
        if (now != null) now.requestFocus();
        handler.removeCallbacks(hideStrip);
        handler.postDelayed(hideStrip, STRIP_MS);
    }

    private void closeStrip() {
        handler.removeCallbacks(hideStrip);
        if (strip == null) return;
        strip.setVisibility(View.GONE);
        if (card == null || card.getVisibility() != View.VISIBLE) tv.requestFocus();
    }

    // ---- Pieces ----

    // "HDMI 3 · Nintendo Switch", or just "HDMI 2" without a CEC device.
    private String title(String id) {
        String device = Inputs.cecDevices(this).get(id);
        String name = Inputs.label(this, id);
        return device == null || device.isEmpty() ? name : name + "  ·  " + device;
    }

    // A pill like on the home screen: glass, white with dark text when focused. on: a green dot (a
    // device is on there).
    private View pill(String label, boolean on, final Runnable click) {
        final TextView t = text(px(Style.LABEL), Style.INK, regular);
        t.setText(label);
        t.setSingleLine(true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(px(28), 0, px(28), 0);
        if (on) {
            t.setCompoundDrawablesRelative(null, null, dot(), null);
            t.setCompoundDrawablePadding(px(12));
        }
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(32));
        bg.setColor(Style.GLASS);
        bg.setStroke(Math.max(1, px(Style.STROKE)), Style.GLASS_STROKE);
        t.setBackground(bg);
        t.setFocusable(true);
        t.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? Style.FOCUS : Style.GLASS);
            t.setTextColor(has ? Style.FOCUS_INK : Style.INK);
            v.animate().scaleX(has ? Style.SCALE_SMALL : 1f).scaleY(has ? Style.SCALE_SMALL : 1f).setDuration(150).start();
        });
        t.setOnClickListener(v -> click.run());
        return t;
    }

    private LinearLayout.LayoutParams pillParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, px(64));
        lp.leftMargin = lp.rightMargin = px(10);
        return lp;
    }

    // The green "something's on" dot, as on the home screen's HDMI pills.
    private GradientDrawable dot() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(0xFF34C759);
        d.setBounds(0, 0, px(14), px(14));
        return d;
    }

    // Glass over the picture: nothing to blur (the picture is on its own plane), so a tint the picture
    // shows through, with the same gloss and rim as everywhere else.
    private LiquidGlass glass(int radius) {
        return new LiquidGlass(px(radius), Math.max(1, px(Style.STROKE)), Style.LIQUID_VIDEO, null, null);
    }

    private TextView text(int size, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        // The glass is light over the picture: a soft shadow keeps white text readable over a bright one.
        t.setShadowLayer(px(6), 0, px(1), 0x99000000);
        return t;
    }

    private void fade(View v, boolean in) {
        if (v == null) return;
        if (in) v.setVisibility(View.VISIBLE);
        v.animate().alpha(in ? 1f : 0f).setDuration(250).withEndAction(() -> {
            if (!in) v.setVisibility(View.GONE);
        }).start();
    }

    private int px(int v) {
        return Math.round(v * scale);
    }
}
