package com.ansub.tvhome;

// One set of sizes and colours for every screen (home screen, menus, Control Center, screensaver), at
// 1920x1080: each screen scales them to the TV with its own px(). From the design in Desing/TV-Launcher
// (tokens.json); change a look here, not in the screens.
final class Style {
    // Corners: tiles, art and the rows inside panels; panels (dock, Control Center, menus); pills and
    // badges are fully round (half their height).
    static final int RADIUS_TILE = 24, RADIUS_PANEL = 40;

    // Glass on the wallpaper (dock, pills, plain tiles), and the dark panels that float over the page.
    static final int GLASS = 0x24FFFFFF, GLASS_STROKE = 0x2EFFFFFF, STROKE = 2;
    static final int PANEL = 0xFF16181F; // solid: anything under it (the clock) would show through
    static final int DIM = 0x99000000; // behind a side panel

    // Liquid glass (menus, panels, Control Center, the app switcher, the dock): a tint over a blurred
    // copy of the page, a gloss on the top half, and a rim lit from the top left that fades round to
    // the bottom right. Over other apps there's no copy to blur, so the tint is stronger (SOLO).
    static final int LIQUID_TINT = 0x8C14161C, LIQUID_SOLO = 0xEE14161C, LIQUID_GLOSS = 0x1AFFFFFF;
    static final int LIQUID_RIM_HI = 0x8CFFFFFF, LIQUID_RIM_LO = 0x12FFFFFF, LIQUID_RIM_END = 0x3DFFFFFF;
    // How bright glass over the page ends up (0 black, 1 white): the tint's strength follows what's under it.
    static final double LIQUID_LEVEL = 0.20;

    // Text on the wallpaper and panels, and focus: white, with dark text and icons on it.
    static final int INK = 0xFFFFFFFF, INK_2 = 0xB8FFFFFF, INK_3 = 0x80FFFFFF;
    static final int FOCUS = 0xFFFFFFFF, FOCUS_INK = 0xFF111318;

    // Type, Roboto: the shelf's name; Control Center's clock; titles and the clock; body; labels.
    static final int DISPLAY = 76, HEADLINE = 48, TITLE = 38, BODY = 29, LABEL = 25;

    // Focus grows tiles a little, pills and rows less.
    static final float SCALE_TILE = 1.10f, SCALE_SMALL = 1.05f;

    // Panels over the page (Control Center, the side lists) and rows in them.
    static final int PANEL_W = 600, PANEL_MARGIN = 64, PANEL_PAD = 32, ROW_H = 84;

    private Style() {}
}
