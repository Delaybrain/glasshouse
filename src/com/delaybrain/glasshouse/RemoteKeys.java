package com.delaybrain.glasshouse;

import android.content.Context;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Which remote button does what, set on the TV under Settings > Remote buttons. Remotes differ: the
// gear sends SETTINGS on one, NOTIFICATION or MENU on the next, and some have a red button where others
// have Apps. Without a choice made on the TV the standard buttons below are used, exactly as before, so
// a TV that already works keeps working. Saved as "code:action,..." in the config prefs (and backups).
final class RemoteKeys {
    static final String HOME = "home", APPS = "apps", CONTROL = "control", LIVE = "live", ACCOUNT = "account";
    // OFF: the button does nothing at all, for one that only gets in the way (on the Peaq, the voice
    // button opens the TV's remote pairing screen when Google's voice search is turned off).
    static final String OFF = "off";
    // In the order the Remote buttons panel lists them, with their names there.
    static final String[] ACTIONS = {HOME, APPS, CONTROL, LIVE, ACCOUNT, OFF};
    static final String[] NAMES = {"Home", "All apps", "Control Center", "Live TV", "Account", "Off"};

    private static final String PREFS = "config", KEY = "keys";
    private static String readFrom; // the saved text the map below was made from
    private static Map<Integer, String> map;

    private RemoteKeys() {}

    // The standard buttons: Home and Apps open this home screen, the gear (SETTINGS on most remotes,
    // NOTIFICATION on the Peaq) Control Center, Guide Live TV, the person button the account page.
    static Map<Integer, String> standard() {
        Map<Integer, String> m = new LinkedHashMap<>();
        m.put(KeyEvent.KEYCODE_HOME, HOME);
        m.put(KeyEvent.KEYCODE_ALL_APPS, APPS);
        m.put(KeyEvent.KEYCODE_SETTINGS, CONTROL);
        m.put(KeyEvent.KEYCODE_NOTIFICATION, CONTROL);
        m.put(KeyEvent.KEYCODE_GUIDE, LIVE);
        m.put(KeyEvent.KEYCODE_PROFILE_SWITCH, ACCOUNT);
        return m;
    }

    // What a button does, or null: then it isn't ours and goes on to the TV untouched. Read on every
    // button press, so the saved text is parsed again only after it changed.
    static synchronized String action(Context c, int code) {
        return all(c).get(code);
    }

    static synchronized Map<Integer, String> all(Context c) {
        String saved = c.getSharedPreferences(PREFS, 0).getString(KEY, null);
        if (map == null || !same(saved, readFrom)) {
            map = saved == null ? standard() : parse(saved);
            readFrom = saved;
        }
        return map;
    }

    // True once buttons were chosen on the TV (not the standard ones).
    static boolean chosen(Context c) {
        return c.getSharedPreferences(PREFS, 0).contains(KEY);
    }

    // The buttons that do action, in the order they were set.
    static List<Integer> buttons(Context c, String action) {
        List<Integer> out = new ArrayList<>();
        for (Map.Entry<Integer, String> e : all(c).entrySet()) if (e.getValue().equals(action)) out.add(e.getKey());
        return out;
    }

    // The button does action from now on, and nothing else it did before.
    static void set(Context c, int code, String action) {
        Map<Integer, String> m = new LinkedHashMap<>(all(c));
        m.remove(code);
        m.put(code, action);
        save(c, m);
    }

    // No button does action any more: those buttons go to the TV as they would without this app.
    static void clear(Context c, String action) {
        Map<Integer, String> m = new LinkedHashMap<>(all(c));
        // An iterator, not removeIf: that needs Android 7, this app runs from Android 5.
        for (java.util.Iterator<String> it = m.values().iterator(); it.hasNext(); ) if (it.next().equals(action)) it.remove();
        save(c, m);
    }

    static void reset(Context c) {
        c.getSharedPreferences(PREFS, 0).edit().remove(KEY).apply();
    }

    private static void save(Context c, Map<Integer, String> m) {
        c.getSharedPreferences(PREFS, 0).edit().putString(KEY, format(m)).apply();
    }

    // Buttons that can't be given a job: moving around, OK and Back are needed to use the TV at all,
    // volume and power belong to the TV, and the digits type the kids lock PIN.
    static boolean choosable(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_VOLUME_UP: case KeyEvent.KEYCODE_VOLUME_DOWN: case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_MUTE: case KeyEvent.KEYCODE_POWER: case KeyEvent.KEYCODE_TV_POWER:
            case KeyEvent.KEYCODE_SLEEP: case KeyEvent.KEYCODE_WAKEUP: case KeyEvent.KEYCODE_UNKNOWN:
                return false;
            default:
                return code < KeyEvent.KEYCODE_0 || code > KeyEvent.KEYCODE_9;
        }
    }

    // The names of the usual remote buttons, by what they look like, in the TV's language (Strings).
    private static final int[] NAMED = {KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_ALL_APPS, KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_NOTIFICATION, KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_PROFILE_SWITCH, KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_PROG_GREEN, KeyEvent.KEYCODE_PROG_YELLOW, KeyEvent.KEYCODE_PROG_BLUE,
            KeyEvent.KEYCODE_TV_INPUT, KeyEvent.KEYCODE_TV, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_CAPTIONS,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE};
    private static final String[] NAMED_AS = {"Home button", "Apps button", "Gear", "Notifications button", "Guide button",
            "Person button", "Menu button", "Red button", "Green button", "Yellow button", "Blue button", "Input button",
            "TV button", "Info button", "Subtitles button", "Play/Pause"};

    // A button's name: the usual ones as above, any other as Android calls it (KEYCODE_BUTTON_5 is "Button 5").
    static String name(int code) {
        for (int i = 0; i < NAMED.length; i++) if (NAMED[i] == code) return Strings.t(NAMED_AS[i]);
        String s = KeyEvent.keyCodeToString(code);
        if (s.startsWith("KEYCODE_")) s = s.substring(8);
        s = s.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String format(Map<Integer, String> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Integer, String> e : m.entrySet()) {
            if (b.length() > 0) b.append(',');
            b.append(e.getKey()).append(':').append(e.getValue());
        }
        return b.toString();
    }

    // Anything that isn't "number:known action" is skipped, so a damaged value can't stop the remote.
    static Map<Integer, String> parse(String s) {
        Map<Integer, String> m = new LinkedHashMap<>();
        for (String part : s.split(",")) {
            int colon = part.indexOf(':');
            if (colon <= 0) continue;
            String action = part.substring(colon + 1).trim();
            if (!known(action)) continue;
            try {
                m.put(Integer.parseInt(part.substring(0, colon).trim()), action);
            } catch (NumberFormatException ignored) {}
        }
        return m;
    }

    private static boolean known(String action) {
        for (String a : ACTIONS) if (a.equals(action)) return true;
        return false;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
