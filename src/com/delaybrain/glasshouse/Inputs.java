package com.delaybrain.glasshouse;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The TV's HDMI inputs, for the home screen's pills and the HDMI player: which there are, what they're
// called, and which have an HDMI-CEC device on (with its name).
final class Inputs {
    static final int MAX = 4;

    // HDMI port -> whether a picture came in when it was last looked at (the home screen's preview, or
    // the HDMI player), and when one was last seen. CEC only knows devices that speak it (a PC doesn't);
    // this is the next best thing. Shared by both, so either one's finding lights the green dot. A plain
    // HDMI input says nothing when its device goes off (this TV calls every port connected), so a picture
    // seen counts only for PICTURE_FRESH_MS.
    static final Map<String, Boolean> SIGNAL = new HashMap<>();
    static final Map<String, Long> SIGNAL_AT = new HashMap<>();
    static final long PICTURE_FRESH_MS = 10 * 60_000;
    private static boolean changed; // the player learnt something the home screen hasn't drawn yet

    private Inputs() {}

    // The HDMI player saw a picture on an input, or saw it go.
    static void picture(String id, boolean seen) {
        if (seen) SIGNAL_AT.put(id, System.currentTimeMillis());
        else SIGNAL_AT.remove(id);
        if (!Boolean.valueOf(seen).equals(SIGNAL.put(id, seen))) changed = true;
    }

    // A picture was seen there lately.
    static boolean pictureSeen(String id) {
        Long at = SIGNAL_AT.get(id);
        return Boolean.TRUE.equals(SIGNAL.get(id)) && at != null && System.currentTimeMillis() - at < PICTURE_FRESH_MS;
    }

    // True once after the player changed what's known: the home screen draws its dots again.
    static boolean takeChanged() {
        boolean c = changed;
        changed = false;
        return c;
    }

    // The HDMI ports, in order (HDMI 1 first), at most MAX. CEC devices show up as inputs of their own
    // under a port (their parent); those aren't ports and are left out here.
    static List<TvInputInfo> hdmi(Context c) {
        List<TvInputInfo> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        for (TvInputInfo in : tim.getTvInputList()) {
            if (in.getType() == TvInputInfo.TYPE_HDMI && in.getParentId() == null && !in.isHidden(c)) out.add(in);
        }
        Collections.sort(out, (a, b) -> a.getId().compareTo(b.getId()));
        return out.size() > MAX ? new ArrayList<>(out.subList(0, MAX)) : out;
    }

    // What can be on an input, picked under Settings > HDMI inputs: a name (in the TV's language) and
    // its icon. Saved by its English name, so a choice stays when the TV's language changes.
    static final String[] KINDS = {"PC", "Laptop", "Game console", "TV box", "Blu-ray player", "Receiver", "Camera"};
    static final int[][] KIND_ICONS = {LineIcon.PICTURE, LineIcon.LAPTOP, LineIcon.GAMEPAD, LineIcon.BOX,
            LineIcon.DISC, LineIcon.SPEAKER, LineIcon.CAMERA};
    private static final String NAMES = "inputNames"; // {"input id": {"name": ..., "kind": ...}}

    // The name given on the TV ("Gaming PC", or a kind like "PC"), or null.
    static String customName(Context c, String id) {
        org.json.JSONObject o = names(c).optJSONObject(id);
        if (o == null) return null;
        String name = o.optString("name", "");
        if (!name.isEmpty()) return name;
        String kind = o.optString("kind", "");
        return kind.isEmpty() ? null : Strings.t(kind);
    }

    // The input's icon: its kind's, else the HDMI plug.
    static int[] icon(Context c, String id) {
        org.json.JSONObject o = names(c).optJSONObject(id);
        String kind = o == null ? "" : o.optString("kind", "");
        for (int i = 0; i < KINDS.length; i++) if (KINDS[i].equals(kind)) return KIND_ICONS[i];
        return LineIcon.HDMI;
    }

    // A kind (one of KINDS), a name of one's own (kind null), or nothing (both null: the TV's name again).
    static void setName(Context c, String id, String kind, String name) {
        org.json.JSONObject all = names(c);
        try {
            if (kind == null && (name == null || name.trim().isEmpty())) all.remove(id);
            else {
                org.json.JSONObject o = all.optJSONObject(id);
                if (o == null) o = new org.json.JSONObject();
                if (kind != null) { o.put("kind", kind); o.remove("name"); }
                if (name != null) o.put("name", name.trim());
                all.put(id, o);
            }
        } catch (org.json.JSONException ignored) {}
        c.getSharedPreferences("config", 0).edit().putString(NAMES, all.toString()).apply();
    }

    private static org.json.JSONObject names(Context c) {
        try {
            return new org.json.JSONObject(c.getSharedPreferences("config", 0).getString(NAMES, "{}"));
        } catch (Exception e) {
            return new org.json.JSONObject();
        }
    }

    // The port's name: the one given in Glasshouse, else the one set in the TV's settings, else the TV's
    // own ("HDMI 2").
    static String label(Context c, TvInputInfo in) {
        String mine = customName(c, in.getId());
        if (mine != null) return mine;
        return tvLabel(c, in);
    }

    // The TV's name for the port, never Glasshouse's ("HDMI 2", or what's set in the TV's settings).
    static String tvLabel(Context c, TvInputInfo in) {
        CharSequence custom = in.loadCustomLabel(c);
        if (custom != null && custom.length() > 0) return withoutDeviceType(custom.toString());
        CharSequence label = in.loadLabel(c);
        return label != null && label.length() > 0 ? withoutDeviceType(label.toString()) : "HDMI";
    }

    static String label(Context c, String inputId) {
        for (TvInputInfo in : hdmi(c)) if (in.getId().equals(inputId)) return label(c, in);
        return "HDMI";
    }

    // HDMI port -> the name of the CEC device that's on there ("Nintendo Switch"). A device that's off,
    // or one that doesn't speak CEC (most PCs), isn't in it.
    static Map<String, String> cecDevices(Context c) {
        Map<String, String> out = new HashMap<>();
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        for (TvInputInfo in : tim.getTvInputList()) {
            if (in.getParentId() == null || in.getType() != TvInputInfo.TYPE_HDMI) continue;
            if (tim.getInputState(in.getId()) == TvInputManager.INPUT_STATE_DISCONNECTED) continue;
            CharSequence name = in.loadLabel(c);
            // CEC names have no spaces when they don't fit ("NintendoSwitch"): put them back.
            out.put(in.getParentId(), name == null ? "" : name.toString().replaceAll("(?<=[a-z])(?=[A-Z])", " "));
        }
        return out;
    }

    // Philips stores a name as "name&DEVICE_TYPE" (like "Computer&COMPUTER"), so show the name only.
    // Only on Philips TVs: elsewhere a name like "AUDIO&VIDEO" is the name itself.
    private static String withoutDeviceType(String label) {
        if (!"Philips".equalsIgnoreCase(android.os.Build.MANUFACTURER)) return label;
        int amp = label.lastIndexOf('&');
        return amp > 0 && label.substring(amp + 1).matches("[A-Z][A-Z0-9_]{2,}") ? label.substring(0, amp).trim() : label;
    }
}
