package com.delaybrain.glasshouse;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
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
    private TimeoutGuard timeouts; // keeps Control Center's screensaver and sleep waits as picked
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
        running = this;
        offTimerAt(getSharedPreferences("config", 0).getLong("offAt", 0)); // a timer set before a restart
        timeouts = new TimeoutGuard(this, handler, () -> front);
        timeouts.start();
        IntentFilter off = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(screenOff, off, RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenOff, off);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        running = null;
        if (timeouts != null) timeouts.stop();
        handler.removeCallbacks(sleepNow); // the saved time stays, for when the service is back
        handler.removeCallbacks(warn);
        try { unregisterReceiver(screenOff); } catch (Exception ignored) {}
        return super.onUnbind(intent);
    }

    // The off timer from Control Center: "turn the TV off in 30 minutes", for falling asleep in front of
    // it. Unlike the sleep setting it doesn't wait for the remote to go quiet, so it works while a film
    // plays. Going to sleep is the accessibility "lock screen" action, which on a TV is standby.
    static HomeGuard running; // set while the service is on, so Control Center can reach it
    private long offAt;       // when the TV goes off, or 0
    private final Runnable sleepNow = () -> {
        offAt = 0;
        if (android.os.Build.VERSION.SDK_INT >= 28) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    };
    private final Runnable warn = () -> android.widget.Toast.makeText(this,
            Strings.t("The TV turns off in a minute"), android.widget.Toast.LENGTH_LONG).show();
    // The TV went off some other way: the timer is done.
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { setOffTimer(0); }
    };

    void setOffTimer(int minutes) {
        offTimerAt(minutes <= 0 ? 0 : System.currentTimeMillis() + minutes * 60_000L);
    }

    // The time is saved, so the timer outlives this service being restarted (an update of the app, or
    // Android stopping it for a moment); onServiceConnected picks it up again.
    private void offTimerAt(long at) {
        handler.removeCallbacks(sleepNow);
        handler.removeCallbacks(warn);
        long left = at - System.currentTimeMillis();
        offAt = left > 0 ? at : 0;
        getSharedPreferences("config", 0).edit().putLong("offAt", offAt).apply();
        if (offAt == 0) return;
        handler.postDelayed(sleepNow, left);
        if (left > 60_000) handler.postDelayed(warn, left - 60_000);
    }

    // Minutes left, rounded up, or 0 when no timer runs.
    int offTimerMinutes() {
        if (offAt == 0) return 0;
        return (int) Math.max(1, (offAt - System.currentTimeMillis() + 59_999) / 60_000);
    }

    // The remote's buttons, before Android hands them on. On Google TV, Home, Apps and Settings go to
    // Google TV's home screen (Settings opens its side panel), so with it off they'd do nothing. Which
    // button does what is in RemoteKeys (standard: Home and Apps open this home screen, Apps with its
    // app list, the gear Control Center, the person button the TV's account page, Guide Live TV, see
    // below). Every other button passes through untouched, the input button too: the TV's own input
    // picker handles it.
    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (learning != null) return learn(e);
        String action = RemoteKeys.action(this, e.getKeyCode());
        if (action == null) return false;
        if (RemoteKeys.OFF.equals(action)) return true; // swallowed: neither we nor the TV do anything
        if (RemoteKeys.LIVE.equals(action)) return guideButton(e);
        boolean home = RemoteKeys.HOME.equals(action);
        // The screensaver ends on any button, but these never reach it: end it here, then do as usual.
        if (e.getAction() == KeyEvent.ACTION_DOWN && Screensaver.active != null) Screensaver.active.finish();
        // Home held down opens the recently used apps instead; its release then does nothing more.
        if (home && e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
            homeHeld = false;
            handler.removeCallbacks(holdHome);
            handler.postDelayed(holdHome, ViewConfiguration.getLongPressTimeout());
        }
        if (home && e.getAction() == KeyEvent.ACTION_UP) {
            handler.removeCallbacks(holdHome);
            if (homeHeld) return true;
        }
        if (e.getAction() != KeyEvent.ACTION_UP || e.isCanceled()) return true;
        handler.removeCallbacks(bringHome);
        Intent i;
        if (RemoteKeys.CONTROL.equals(action)) {
            i = new Intent(this, ControlCenter.class);
        } else if (RemoteKeys.ACCOUNT.equals(action)) {
            i = accountSettings(this);
        } else {
            i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            if (RemoteKeys.APPS.equals(action)) i.putExtra("show", "apps");
        }
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) {}
        return true;
    }

    // "Account & sign-in", where Google TV's profiles live, else the TV's settings.
    static Intent accountSettings(Context c) {
        Intent i = new Intent(Settings.ACTION_SYNC_SETTINGS);
        return i.resolveActivity(c.getPackageManager()) != null ? i : new Intent(Settings.ACTION_SETTINGS);
    }

    // Settings > Remote buttons waits for a button: the next one released goes to it instead of doing
    // anything, whatever it does now. Buttons that can't be chosen (arrows, OK, Back) work as usual, so
    // Back still ends the wait.
    interface Learner { void got(int code); }
    static Learner learning;

    private boolean learn(KeyEvent e) {
        final int code = e.getKeyCode();
        if (!RemoteKeys.choosable(code)) return false;
        if (e.getAction() == KeyEvent.ACTION_UP && e.getRepeatCount() == 0) {
            final Learner l = learning;
            learning = null;
            handler.post(() -> l.got(code));
        }
        return true;
    }

    // The guide button (a TV on the Peaq remote): opens Live TV. In Live TV it's passed on, so Live TV
    // can show its own guide; MediaTek doesn't let other apps open that guide themselves.
    private boolean guideButton(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN && Screensaver.active != null) Screensaver.active.finish();
        Intent live = MainActivity.liveTvIntent(this);
        if (live == null || (live.getComponent() != null && front.equals(live.getComponent().getPackageName())))
            return false;
        if (e.getAction() == KeyEvent.ACTION_UP && !e.isCanceled()) {
            handler.removeCallbacks(bringHome);
            start(live);
        }
        return true;
    }

    private String front = ""; // the app in front, from window changes

    private boolean homeHeld; // Home was held long enough to open the recent apps
    private final Runnable holdHome = () -> {
        homeHeld = true;
        handler.removeCallbacks(bringHome);
        start(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .putExtra("show", "recents"));
    };

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
        if (VOICE_PAIRING.equals(pkg + "/" + e.getClassName()) && !voiceSearchOn()) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            return;
        }
        // Kids lock: a locked app that wasn't opened with the PIN gets the PIN pad over it.
        if (!pkg.equals(getPackageName()) && KidLock.isLocked(this, pkg) && !pkg.equals(KidLock.unlocked)) {
            start(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    .putExtra("show", "pin").putExtra("pkg", pkg));
            return;
        }
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

    // The voice button on MediaTek TVs (Peaq, Changhong): with the remote not paired over Bluetooth it
    // opens this page, to pair it for voice. The button is the TV's own (a "global key"), so it never
    // gets to HomeGuard and can't be given a job or turned off under Remote buttons. With Google's voice
    // search turned off there's nothing to pair it for: the page is closed again as soon as it shows.
    private static final String VOICE_PAIRING = "com.mediatek.wwtv.setupwizard/com.mediatek.wwtv.setupwizard.BluetoothActivityVoice";

    private boolean voiceSearchOn() {
        try {
            return getPackageManager().getApplicationInfo("com.google.android.katniss", 0).enabled;
        } catch (Exception notThere) {
            return false;
        }
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(bringHome);
    }
}
