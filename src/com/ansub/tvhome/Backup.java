package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

// What a backup holds: the choices made on the TV (apps and their order, looks, status bar, screensaver,
// kids lock). Not the timers or what was last open. As JSON, for the file on the TV (backup.sh, the
// Settings menu) and for a phone or computer over Wi-Fi (PhotoDrop).
final class Backup {
    private static final String PREFS = "config";
    private static final String[] KEYS = {"apps", "quick", "wallpaper", "tiles", "tilesBrand", "status",
            "saver", "pin", "locked"};

    private Backup() {}

    static String json(Context c) throws org.json.JSONException {
        JSONObject o = new JSONObject();
        java.util.Map<String, ?> all = c.getSharedPreferences(PREFS, 0).getAll();
        for (String k : KEYS) if (all.containsKey(k)) o.put(k, all.get(k));
        return o.toString(2);
    }

    // Puts a backup's choices in place of the ones now (a choice the backup doesn't have goes back to its
    // default). False, and nothing changed, when it isn't a backup.
    static boolean apply(Context c, String json) {
        JSONObject o;
        try {
            o = new JSONObject(json);
        } catch (Exception notJson) {
            return false;
        }
        boolean any = false;
        for (String k : KEYS) any |= o.has(k);
        if (!any) return false; // some other JSON file
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, 0).edit();
        for (String k : KEYS) {
            e.remove(k);
            if (!o.has(k)) continue;
            Object v = o.opt(k);
            if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
            else e.putString(k, String.valueOf(v));
        }
        return e.commit();
    }
}
