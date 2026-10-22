package com.ansub.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

// Keeps this home screen in front on Google TV, where the TV's own home screen has to stay on: turning
// it off has left some Google TVs stuck restarting. Whenever another home screen's Home page comes to
// the front (the Home button, the end of an app, a restart), this one is brought up instead.
//
// Only the other launchers' Home pages count, not their other screens (like Google TV's settings panel),
// and a short wait lets an app that a remote button (YouTube, Netflix) starts through the TV's home screen
// come up first: if anything else reaches the front in that time, nothing happens.
//
// Turn it on with:  sh homeguard.sh <TV_IP> on    and off again with  off.  Nothing else changes.
public class HomeGuard extends AccessibilityService {
    private static final long WAIT_MS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> otherHomes = new HashSet<>();
    private final Runnable bringHome = () -> {
        Intent i = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(i); } catch (Exception ignored) {}
    };

    // What it listens to is set in res/xml/homeguard.xml: key filtering can only be asked for there.
    @Override
    protected void onServiceConnected() {
        findOtherHomes();
    }

    // Home, Apps, Settings, the person and TV buttons and Guide, before Android hands them on. On Google TV these
    // go to Google TV's home screen (Settings opens its side panel), so with it off they'd do nothing.
    // Home and Apps open this home screen (Apps with its app list), Settings opens Control Center, the
    // person button the TV's account page, Guide the TV guide. Everything else passes through untouched.
    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.KEYCODE_TV_INPUT) return tvButton(e);
        // The gear button sends NOTIFICATION on some remotes (Peaq), SETTINGS on others.
        boolean settings = code == KeyEvent.KEYCODE_SETTINGS || code == KeyEvent.KEYCODE_NOTIFICATION;
        boolean profile = code == KeyEvent.KEYCODE_PROFILE_SWITCH;
        if (code != KeyEvent.KEYCODE_HOME && code != KeyEvent.KEYCODE_ALL_APPS && !settings && !profile
                && code != KeyEvent.KEYCODE_GUIDE) return false;
        if (e.getAction() != KeyEvent.ACTION_UP || e.isCanceled()) return true;
        handler.removeCallbacks(bringHome);
        Intent i;
        if (settings) {
            i = new Intent(this, ControlCenter.class);
        } else if (profile) {
            // "Account & sign-in", where Google TV's profiles live.
            i = new Intent(Settings.ACTION_SYNC_SETTINGS);
            if (i.resolveActivity(getPackageManager()) == null) i = new Intent(Settings.ACTION_SETTINGS);
        } else if (code == KeyEvent.KEYCODE_GUIDE) {
            // MediaTek's Live TV has its guide as an activity of its own; else just open Live TV.
            i = new Intent(GUIDE).setPackage(MEDIATEK_TV);
            if (i.resolveActivity(getPackageManager()) == null) i = MainActivity.liveTvIntent(this);
            if (i == null) return true;
        } else {
            i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            if (code == KeyEvent.KEYCODE_ALL_APPS) i.putExtra("show", "apps");
        }
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) {}
        return true;
    }

    private static final String MEDIATEK_TV = "com.mediatek.wwtv.tvcenter";
    private static final String GUIDE = "com.mediatek.wwtv.tvcenter.saepg.activity";
    private static final String VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS"; // the TV's input picker
    private boolean inputsShown; // the TV button was held long enough to open the input picker
    private String front = "";   // the app in front, from window changes

    // The TV button (KEYCODE_TV_INPUT, a TV with an arrow): a press opens Live TV, or the input picker
    // when Live TV is already in front; holding it opens the input picker straight away.
    private boolean tvButton(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() == 0) inputsShown = false;
            else if (!inputsShown && e.getEventTime() - e.getDownTime() >= 500) {
                inputsShown = true;
                start(new Intent(VIEW_INPUTS));
            }
            return true;
        }
        if (e.isCanceled() || inputsShown) return true;
        Intent live = MainActivity.liveTvIntent(this);
        boolean inLiveTv = live != null && live.getComponent() != null
                && front.equals(live.getComponent().getPackageName());
        start(live == null || inLiveTv ? new Intent(VIEW_INPUTS) : live);
        return true;
    }

    private void start(Intent i) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) {}
    }

    // Every Home page on the TV but this one, by package/class. Settings and setup apps answer Home too,
    // as a fallback the TV needs to start, so they're left alone.
    private void findOtherHomes() {
        otherHomes.clear();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo ri : getPackageManager().queryIntentActivities(home, 0)) {
            String p = ri.activityInfo.packageName;
            if (p.equals(getPackageName()) || p.contains("setup") || p.contains("wraith")
                    || p.contains("provision") || p.endsWith(".settings")) continue;
            otherHomes.add(p + "/" + ri.activityInfo.name);
            // An alias reports the activity behind it once it's open.
            if (ri.activityInfo.targetActivity != null) otherHomes.add(p + "/" + ri.activityInfo.targetActivity);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || e.getPackageName() == null || e.getClassName() == null) return;
        String pkg = e.getPackageName().toString();
        if (!pkg.equals("com.android.systemui")) front = pkg; // System UI is only a toast or the volume bar
        if (pkg.equals(getPackageName())) {
            handler.removeCallbacks(bringHome);
            return;
        }
        String at = pkg + "/" + e.getClassName();
        if (otherHomes.contains(at)) {
            handler.removeCallbacks(bringHome);
            handler.postDelayed(bringHome, WAIT_MS);
        } else if (!pkg.equals("com.android.systemui")) {
            // Another app came up: the Home page was only on the way to it. System UI is only a toast
            // or the volume bar, so it doesn't count.
            handler.removeCallbacks(bringHome);
        }
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(bringHome);
    }
}
