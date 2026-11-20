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

    // The port's name: the one set in the TV's settings, else the TV's own ("HDMI 2").
    static String label(Context c, TvInputInfo in) {
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
