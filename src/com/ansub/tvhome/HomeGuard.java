package com.ansub.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
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

    @Override
    protected void onServiceConnected() {
        // Set here instead of in an XML file, so the app still needs no resources.
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 0;
        setServiceInfo(info);
        findOtherHomes();
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
