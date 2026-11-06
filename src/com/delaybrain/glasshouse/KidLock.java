package com.delaybrain.glasshouse;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Kids lock: apps that open only with a 4-digit PIN. The home screen asks before it opens one; HomeGuard
// covers one that came up any other way (voice, a link, the TV's menus) with the PIN pad. The PIN is
// kept as a hash, never as itself. Both run in this app's process, so they share unlocked.
final class KidLock {
    private static final String PREFS = "config", PIN = "pin", LOCKED = "locked";

    // The locked app opened with the PIN, until the home screen comes back.
    static String unlocked;

    private KidLock() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, 0);
    }

    static boolean hasPin(Context c) {
        return prefs(c).getString(PIN, null) != null;
    }

    static boolean check(Context c, String pin) {
        String saved = prefs(c).getString(PIN, null);
        return saved != null && saved.equals(hash(pin));
    }

    static void setPin(Context c, String pin) {
        prefs(c).edit().putString(PIN, hash(pin)).apply();
    }

    // Off: no PIN and nothing locked.
    static void clear(Context c) {
        prefs(c).edit().remove(PIN).remove(LOCKED).apply();
        unlocked = null;
    }

    static Set<String> locked(Context c) {
        String saved = prefs(c).getString(LOCKED, "");
        Set<String> out = new HashSet<>(Arrays.asList(saved.split(",")));
        out.remove("");
        return out;
    }

    static void setLocked(Context c, Set<String> pkgs) {
        prefs(c).edit().putString(LOCKED, android.text.TextUtils.join(",", pkgs)).apply();
    }

    // Only with a PIN set: a locked list left behind without one locks nothing.
    static boolean isLocked(Context c, String pkg) {
        return pkg != null && hasPin(c) && locked(c).contains(pkg);
    }

    private static String hash(String pin) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(("glasshouse:" + pin).getBytes("UTF-8"));
            StringBuilder s = new StringBuilder();
            for (byte b : d) s.append(String.format("%02x", b));
            return s.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e); // SHA-256 is always there
        }
    }
}
