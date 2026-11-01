package com.ansub.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.List;

public class MainActivity extends Activity {

    // Top-row apps, in this order of preference:
    //  1. a list saved at install time:  am start -n com.ansub.tvhome/.MainActivity --es apps "pkg1,pkg2"
    //     (--es apps auto  goes back to 2; add --es stamp <unique> so it's applied only once, see saveConfig)
    //  2. apps.txt (built in as Apps.LIST), topped up with other TV apps installed on this TV.
    // Missing apps are skipped. Topping up stops at six apps, but a chosen list can be any length:
    // six show, and the row scrolls sideways for the rest (Right on the last goes to the first, Left on
    // the first to the last).
    // On the TV, hold OK on an app (or press Menu) to move or remove it, or add another; that saves list 1.
    // A network tile always sits at the top right, level with the clock. Settings shortcuts can join
    // it there, off until picked the same way:
    //   --es quick "bluetooth,sound,picture"
    // (--es quick none  takes them away again; "wifi" and "ethernet" are the network tile now).
    private static final String PREFS = "config";
    private static final List<String> NOT_APPS = Arrays.asList("com.android.vending", "com.android.tv.settings",
            "com.android.tv", "com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx",
            "com.google.android.leanbacklauncher");
    private static final String PLAY_STORE = "com.android.vending";
    private static final String SETTINGS = "settings";
    private static final String ADD = "add";
    private static final String LIVE = "live";
    private static final String[] ADD_ITEMS = {"Add an app", "Arrange apps"};
    private static final String[] EDIT_ITEMS ={"Move", "Replace", "Remove from home", "Uninstall"};
    private static final String[] EDIT_ITEMS_SYSTEM = {"Move", "Replace", "Remove from home", "App info"};
    private static final int PER_ROW = 6, MAX_INPUTS = 4;

    // Settings shortcuts: name, label, icon. Tile keys are QUICK + name.
    private static final String QUICK = "quick:";
    private static final String[] QUICK_NAMES = {"bluetooth", "sound", "picture"};
    private static final String[] QUICK_LABELS = {"Bluetooth", "Sound", "Picture"};
    private static final int[][] QUICK_ICONS = {LineIcon.BLUETOOTH, LineIcon.SOUND, LineIcon.PICTURE};
    private static final String NETWORK = "network";
    private static final int OFFLINE = 0, ETHERNET = 4; // network states; 1 to 3 are Wi-Fi bars
    // TCL keeps its own Picture and Sound panels in a service; Android's Sound page is missing there.
    private static final String TCL_SHOW_WINDOW = "com.tcl.settings.SHOW_WINDOW";

    // Layout is designed at 1920x1080 and scaled to the TV's real UI size by px(). It follows variant B
    // ("compact glass dock") of the design in Desing/TV-Launcher: the glass dock holds a row of apps
    // and, under it, the inputs as pills. More rows than fit scroll the shelf away, like Apple TV.
    // The dock is DOCK_X from the sides and the bottom alike. Everything lines up with the dock's inner
    // edges (MARGIN from both sides): name and first tile on the left, art, clock and last tile on the
    // right. Six tiles and five gaps fill the dock's inside exactly: 6 x 262 + 5 x 28 = 1712.
    private static final int DOCK_X = 64, DOCK_PAD = 40, DOCK_RADIUS = Style.RADIUS_PANEL, MARGIN = DOCK_X + DOCK_PAD;
    private static final int TILE_W = 262, TILE_H = 147, GAP = 28, RADIUS = Style.RADIUS_TILE;
    private static final int PILL_H = 64, PILL_GAP = 20, PILL_PAD = 28, PILL_ICON = 28, PILL_ROOM = 96; // ROOM: tile to pills
    private static final int DOCK_TOP = 1080 - DOCK_X - (DOCK_PAD + TILE_H + PILL_ROOM + PILL_H + DOCK_PAD);
    private static final int TILE_LEFT = MARGIN, ROW1_TOP = DOCK_TOP + DOCK_PAD;
    private static final int CLOCK_BOTTOM = 99; // where the top row ends, for placing the art below it
    private static final int ICON_TOP = 40, ICON_BUTTON = 72; // the top row: its top, and its height
    private static final int STATUS_ICON = 30; // the network icon, as tall as the status text
    private static final float ICON_DIM = 0xB8 / (float) 0xEB; // brings LineIcon's white down to Style.INK_2
    private static final String STATUS_DOT = "   \u00b7   "; // between the items of the top rows
    // The art sits halfway between the clock and the dock; the name is level with the art's middle.
    private static final int ART_W = 640, ART_H = 360, ART_RADIUS = Style.RADIUS_TILE;
    private static final int ART_TOP = (CLOCK_BOTTOM + DOCK_TOP - ART_H) / 2;
    private static final int TITLE_TOP = ART_TOP + ART_H / 2 - 69, SUB_TOP = TITLE_TOP + 100; // 69: half the name + line
    private static final int SHELF_H = DOCK_TOP;
    private static final float FOCUS_SCALE = Style.SCALE_TILE;

    // The Settings tile opens a small menu: the home screen's own settings, then the TV's.
    private static final String[] SETTINGS_ITEMS = {"Wallpaper", "Tiles", "Status bar", "Brightness", "TV settings"};
    // How app banners look ("Tiles" in that menu): as the apps made them, all on dark, all on light, the
    // logos straight on the glass, or dark with only the focused app in colour (black & white) or lit (spotlight).
    private static final String[] TILE_TONES = {"Original", "Dark", "Light", "Glass", "Black & white", "Spotlight",
            "Light by day, dark at night"};
    private static final int TONE_ORIGINAL = 0, TONE_DARK = 1, TONE_LIGHT = 2, TONE_GLASS = 3, TONE_MONO = 4,
            TONE_SPOTLIGHT = 5, TONE_AUTO = 6;
    private static final int DAY_FROM = 7, DAY_UNTIL = 19; // the Auto look's day, in hours
    private static final int RECENTS = 6; // apps in the list Home held opens
    // What the top of the page shows ("Status bar" in that menu). Clock and network sit at the top right,
    // the rest is one line at the top left. The off timer shows only while one is set.
    private static final String[] STATUS_ITEMS = {"Clock", "Network", "Date", "CPU", "RAM", "RAM in GB",
            "Download speed", "Off timer"};
    private static final String[] STATUS_LABELS = {"Clock", "Network", "Date", "CPU clock", "RAM usage",
            "RAM in GB", "Download speed", "Off timer"}; // what the menu calls them; the items are the saved names
    private static final String STATUS_DEFAULT = "Clock,Network,CPU,RAM,Off timer";
    // Android TV 11 and later (Google TV too) has no Display page, but its "Display & Sound" page holds Picture.
    private static final String[] TV_DISPLAY_SOUND = {"com.android.tv.settings",
            "com.android.tv.settings.device.displaysound.DisplaySoundActivity"};
    // Wallpapers, saved by name: a gradient top to bottom, then a glow from the top left and one from
    // the bottom right. Dark ones first, then bright ones that get darker towards the bottom, so the
    // white labels stay easy to read. Only colours, no images, so every one costs the same as Dusk,
    // the default.
    private static final String[] WALLPAPER_NAMES = {"Dusk", "Midnight", "Plum", "Forest", "Graphite", "Ocean", "Ember",
            "Sunset", "Lagoon", "Aurora", "Coral", "Iris", "Meadow", "Gold"};
    private static final int[][] WALLPAPERS = {
        {0xFF15100F, 0xFF0A0809, 0xFF050405, 0x36FF8048, 0x2EAA3C8C},
        {0xFF0E1220, 0xFF080A13, 0xFF040509, 0x384A6CFF, 0x2E5A3CC8},
        {0xFF170E19, 0xFF0C070D, 0xFF050306, 0x38C050B4, 0x2E6440D2},
        {0xFF0C150F, 0xFF070C09, 0xFF030604, 0x3050B478, 0x28289682},
        {0xFF151617, 0xFF0B0C0D, 0xFF050506, 0x1CFFFFFF, 0x16A0B4C8},
        {0xFF0A1517, 0xFF060C0E, 0xFF030607, 0x3426B4C8, 0x2E2A64C0},
        {0xFF190D08, 0xFF0D0605, 0xFF060303, 0x40FF5A24, 0x32C8283C},
        {0xFF8C3A5E, 0xFF5E2348, 0xFF22101E, 0xA0FF8A4C, 0x80E0457A},
        {0xFF14588E, 0xFF15366E, 0xFF0A1632, 0x9030C8E0, 0x807A4CE0},
        {0xFF0F4048, 0xFF0F2E3A, 0xFF08141C, 0x9038E0A0, 0x90A050F0},
        {0xFFC0604E, 0xFF8A3456, 0xFF2E1230, 0x90FFB070, 0x70E04090},
        {0xFF5A3CAE, 0xFF3A2A84, 0xFF16123A, 0x90C080FF, 0x804A7CFF},
        {0xFF2E6E4C, 0xFF1C4A40, 0xFF0A201E, 0x90A8E070, 0x6030A8A8},
        {0xFFB07624, 0xFF86401E, 0xFF2A1410, 0x90FFD060, 0x70FF5040},
    };
    private float scale = 1f;

    private static Typeface regular, semibold;

    private FrameLayout root; // the page, inside a vertical scroller
    private ImageView shelf;      // the focused app's art, drawn tiny and stretched, so it reads as a blur
    private ImageView shelfArt;   // the same art, sharp, at the right of the shelf
    private GradientDrawable artShape; // its background: dark, or glass for the Glass look
    private InputPreview preview; // in the art's place for HDMI inputs: what's coming in
    private View wallpaperView;   // the wallpaper, behind everything but the HDMI preview
    private View dimView;         // darkens the HDMI preview's picture, so the page on top stays readable
    // HDMI input -> whether its last preview showed a picture. CEC only knows devices that speak it (a
    // PC doesn't); this is the next best thing, learnt each time a pill is looked at.
    private final java.util.Map<String, Boolean> signal = new java.util.HashMap<>();
    private TextView shelfTitle, shelfSub;
    private String shelfKey;      // what the shelf shows now, so focusing the same tile does nothing
    private final java.util.Map<String, Long> lastUsed = new java.util.HashMap<>(); // package -> last seen
    private final java.util.Map<String, String> cecDevices = new java.util.HashMap<>(); // HDMI input -> device on it
    private View dock;            // the glass panel the tiles sit on
    private View dockShade;       // darkens the dock over a live HDMI picture
    private android.widget.HorizontalScrollView appScroll; // the app row, scrolling sideways
    private FrameLayout appStrip; // the app tiles, in appScroll
    private int stripX, stripY;   // where appScroll sits, so tiles can be placed inside it
    private ImageView moreLeft, moreRight; // ‹ › beside the app row when apps are out of sight
    private static final int ROW_GROW = 16, ARROW = 24; // room for a focused tile to grow; arrow size

    private ImageView arrow(int[] icon) {
        ImageView v = new ImageView(this);
        LineIcon line = new LineIcon(icon);
        line.setAlpha(0xB8); // Style.INK_2
        v.setImageDrawable(line);
        v.setAlpha(0f);
        root.addView(v, at(0, 0, 0, 0)); // placed by buildTiles
        return v;
    }
    private LinearLayout liveBadge; // "● LIVE · 1080p" at the top right while an HDMI picture shows
    private TextView liveText;
    private TextClock clockView;
    private int tileW, tileH, gridTop; // app grid geometry, set by buildTiles
    private final List<View> tiles = new ArrayList<>();
    private boolean dirty = true;
    private java.util.Locale shownLocale; // the TV's language (and region) when the page was built
    private String focusedKey;
    private final List<String> rowApps = new ArrayList<>(); // the top row as shown, in order
    private String movingKey;   // the app being moved with left/right, if any
    private TextView moveHint;  // shown above the apps while moving
    private boolean longPressed; // OK was held: swallow the key-up so the app doesn't open
    // The tile OK went down on. An app that closes on OK's key-down (Netflix's "Exit") hands the matching
    // key-up to the tile still focused here, so a key-up only opens the app it went down on (#30).
    private String okDownKey;

    // Apps installed, removed or updated. This often lands once Home is already back in front (an app
    // uninstalled from the menu is gone only after Android's prompt has closed), so catch up then.
    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            shelfCache.clear(); // an updated app may have new art
            dirty = true;
            refreshApps();
        }
    };

    // The network tile follows the connection live: Wi-Fi bars, Ethernet or Offline. Only its icon and
    // label change, so nothing is rebuilt.
    private ImageView netIcon;
    private int netState = -1;
    private final ConnectivityManager.NetworkCallback networkChanged = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { runOnUiThread(MainActivity.this::refreshNetwork); }
        @Override public void onLost(Network n) { runOnUiThread(MainActivity.this::refreshNetwork); }
        // Also fires when the Wi-Fi signal gets stronger or weaker.
        @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
            runOnUiThread(MainActivity.this::refreshNetwork);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        saveConfig(getIntent());
        loadFonts();
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        // The wallpaper is a view under the page, not the window's background, so it can fade out and
        // show the HDMI preview's live picture underneath (see onPicture).
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        wallpaperView = new View(this);
        dimView = new View(this);
        // Darker where the page has something to read (the name on the left, the dock at the bottom),
        // lighter at the top right, so the picture stays bright where nothing covers it.
        GradientDrawable dimSide = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0x99000000, 0x33000000, 0x00000000});
        GradientDrawable dimDown = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x40000000, 0x0D000000, 0x8C000000, 0xE6000000});
        dimView.setBackground(new LayerDrawable(new Drawable[] {dimSide, dimDown}));
        dimView.setAlpha(0f);
        showWallpaper(wallpaper());

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setClipChildren(false);
        root = new FrameLayout(this);
        root.setClipChildren(false);
        scroller.addView(root, new FrameLayout.LayoutParams(-1, -2));
        shownLocale = java.util.Locale.getDefault();

        // Top shelf, back to front: a soft wash of the art's colours, a scrim for the clock, the art itself
        // (sharp, at the right, like Apple TV), then the name.
        shelf = new ImageView(this);
        shelf.setScaleType(ImageView.ScaleType.FIT_XY);
        // The wash is blurred and faded out in its small bitmap (see shelfWash), not with RenderEffect:
        // that one draws past the view's edges, as a band under the shelf.
        FrameLayout shelfFrame = new FrameLayout(this);
        shelfFrame.addView(shelf, new FrameLayout.LayoutParams(-1, -1));
        root.addView(shelfFrame, at(0, 0, -1, px(SHELF_H)));

        shelfArt = new ImageView(this);
        shelfArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artShape = new GradientDrawable();
        artShape.setCornerRadius(px(ART_RADIUS));
        shelfArt.setBackground(artShape);
        shelfArt.setClipToOutline(true);
        // No elevation shadow: real shadows are re-drawn every frame and this TV's GPU can't afford them.
        shelfArt.setAlpha(0f);
        int sw = getResources().getDisplayMetrics().widthPixels;
        root.addView(shelfArt, at(sw - px(MARGIN) - px(ART_W), px(ART_TOP), px(ART_W), px(ART_H)));
        // HDMI inputs show what's coming in, live, behind the whole page.
        preview = new InputPreview(this, new InputPreview.Listener() {
            @Override public void onSignal(String inputId, boolean picture) { MainActivity.this.onSignal(inputId, picture); }
            @Override public void onPicture(boolean showing) { MainActivity.this.onPicture(showing); }
        });

        shelfTitle = text(px(Style.DISPLAY), Color.WHITE, semibold);
        shelfTitle.setLetterSpacing(-0.02f);
        shelfTitle.setSingleLine(true);
        shelfTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        shelfTitle.setIncludeFontPadding(false);
        int textW = sw - 2 * px(MARGIN) - px(ART_W) - px(48); // up to the art
        root.addView(shelfTitle, at(px(MARGIN), px(TITLE_TOP), textW, -2));
        shelfSub = text(px(Style.BODY), Style.INK_2, regular);
        shelfSub.setSingleLine(true);
        shelfSub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(shelfSub, at(px(MARGIN), px(SUB_TOP), textW, -2));

        // Under the glass: a dark tint that comes in over a live HDMI picture, so the tiles don't get
        // lost in a moving image.
        dockShade = new View(this);
        GradientDrawable shade = new GradientDrawable();
        shade.setCornerRadius(px(DOCK_RADIUS));
        shade.setColor(0x73000000);
        dockShade.setBackground(shade);
        dockShade.setAlpha(0f);
        root.addView(dockShade, at(0, 0, 0, 0)); // placed by buildTiles, with the dock

        dock = new View(this);
        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(px(DOCK_RADIUS));
        glass.setColor(Style.GLASS);
        glass.setStroke(Math.max(1, px(2)), Style.GLASS_STROKE);
        dock.setBackground(glass);
        root.addView(dock, at(0, 0, 0, 0)); // placed by buildTiles

        // The apps: one row that scrolls sideways inside the dock when there are more than fit, so the
        // pills under it never move. It cuts off at the dock's edges, where the next app peeks in.
        // Android scrolls it along with the focus.
        appScroll = new android.widget.HorizontalScrollView(this) {
            // No scrolling of its own to the focused tile (it stops just short, off the grid):
            // scrollRowTo lines the tiles up with the dock instead.
            @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) { return 0; }
        };
        // Cut off at its own edges (root doesn't clip, so focused tiles can grow out of their places).
        appScroll.setOutlineProvider(android.view.ViewOutlineProvider.BOUNDS);
        appScroll.setClipToOutline(true);
        appScroll.setHorizontalScrollBarEnabled(false);
        appScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        appScroll.setSmoothScrollingEnabled(true);
        appStrip = new FrameLayout(this);
        appStrip.setClipChildren(false);
        appStrip.setClipToPadding(false); // the padding after the last app is room for it to grow into
        appScroll.addView(appStrip, new FrameLayout.LayoutParams(-2, -1));
        root.addView(appScroll, at(0, 0, 0, 0)); // placed by buildTiles
        moreLeft = arrow(LineIcon.PREV);
        moreRight = arrow(LineIcon.NEXT);

        // The top of the page, in one look: a line at the top left (date, CPU, RAM, ...) and its mirror at
        // the top right (download speed · network · clock), same size, colour and dots between. Bluetooth,
        // sound and picture live in Control Center.
        statusLine = statusText();
        root.addView(statusLine, at(px(MARGIN), px(ICON_TOP), -2, px(ICON_BUTTON)));
        topRight = new LinearLayout(this);
        topRight.setOrientation(LinearLayout.HORIZONTAL);
        topRight.setGravity(Gravity.CENTER_VERTICAL);
        topRight.setClipChildren(false);
        rateText = statusText();
        rateDot = statusText();
        rateDot.setText(STATUS_DOT);
        clockDot = statusText();
        clockDot.setText(STATUS_DOT);
        clockView = clock("h:mm", "H:mm", px(Style.LABEL), Style.INK_2, semibold);
        clockView.setFontFeatureSettings("tnum");
        for (View v : new View[] {rateText, rateDot, clockDot, clockView}) topRight.addView(v); // network goes in at 2
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, px(ICON_BUTTON), Gravity.TOP | Gravity.END);
        tp.topMargin = px(ICON_TOP);
        tp.rightMargin = px(MARGIN);
        root.addView(topRight, tp);

        // "● LIVE · 1080p · 60 Hz", where an app's art goes, while an HDMI input's picture is behind the page.
        liveBadge = new LinearLayout(this);
        liveBadge.setOrientation(LinearLayout.HORIZONTAL);
        liveBadge.setGravity(Gravity.CENTER_VERTICAL);
        liveBadge.setPadding(px(22), 0, px(24), 0);
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(px(24));
        badge.setColor(0x66000000);
        badge.setStroke(Math.max(1, px(Style.STROKE)), Style.GLASS_STROKE);
        liveBadge.setBackground(badge);
        View redDot = new View(this);
        GradientDrawable red = new GradientDrawable();
        red.setShape(GradientDrawable.OVAL);
        red.setColor(0xFFFF3B30); // Apple's red
        redDot.setBackground(red);
        liveBadge.addView(redDot, new LinearLayout.LayoutParams(px(14), px(14)));
        liveText = text(px(Style.LABEL), Color.WHITE, semibold);
        liveText.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-2, -2);
        ll.leftMargin = px(12);
        liveBadge.addView(liveText, ll);
        liveBadge.setAlpha(0f);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, px(48), Gravity.TOP | Gravity.END);
        bp.topMargin = px(ART_TOP);
        bp.rightMargin = px(MARGIN);
        root.addView(liveBadge, bp);

        moveHint = text(px(Style.LABEL), Style.INK_2, regular);
        moveHint.setText(Strings.t("Use the arrows to move it, then press OK"));
        moveHint.setVisibility(View.GONE);
        root.addView(moveHint, at(px(MARGIN), px(ROW1_TOP - 64), -2, -2));

        FrameLayout screen = new FrameLayout(this);
        screen.addView(preview.view(), new FrameLayout.LayoutParams(-1, -1)); // under everything
        screen.addView(wallpaperView, new FrameLayout.LayoutParams(-1, -1));
        screen.addView(dimView, new FrameLayout.LayoutParams(-1, -1));
        screen.addView(scroller, new FrameLayout.LayoutParams(-1, -1));
        setContentView(screen);
        showRequested(getIntent());

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(packagesChanged, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(packagesChanged, f);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) try {
            if (android.os.Build.VERSION.SDK_INT >= 24) cm.registerDefaultNetworkCallback(networkChanged);
            else cm.registerNetworkCallback(new NetworkRequest.Builder().build(), networkChanged);
        } catch (Exception ignored) {}

        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim != null) try { tim.registerCallback(inputsChanged, new android.os.Handler(getMainLooper())); }
        catch (Exception ignored) {}
    }

    // An HDMI-CEC device turned on or off (Android adds or removes its input): redraw the pills' dots.
    // Waits a second, as a device coming up sends several of these at once. A device that just came on
    // while the home screen is in front gets the focus, and the shelf offers to switch to it.
    private final Runnable inputsSettled = () -> {
        java.util.Set<String> before = new java.util.HashSet<>(cecDevices.keySet());
        refreshCecDevices();
        String on = null;
        for (String input : cecDevices.keySet()) if (!before.contains(input)) on = input;
        // A device that went off takes its "picture seen" with it, or that would keep the dot lit.
        for (String input : before) if (!cecDevices.containsKey(input)) signal.remove(input);
        if (on != null && hasWindowFocus() && movingKey == null) {
            deviceJustOn = on;
            focusedKey = on;
        }
        dirty = true;
        refreshApps();
    };
    private String deviceJustOn; // the HDMI input whose device was just switched on, until focus moves on
    private final TvInputManager.TvInputCallback inputsChanged = new TvInputManager.TvInputCallback() {
        @Override public void onInputAdded(String id) { changed(); }
        @Override public void onInputRemoved(String id) { changed(); }
        @Override public void onInputStateChanged(String id, int state) { changed(); }
        private void changed() {
            root.removeCallbacks(inputsSettled);
            root.postDelayed(inputsSettled, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) try { cm.unregisterNetworkCallback(networkChanged); } catch (Exception ignored) {}
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim != null) try { tim.unregisterCallback(inputsChanged); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (saveConfig(intent)) buildTiles();
        showRequested(intent);
    }

    // HomeGuard opens the app list for the remote's Apps button with --es show apps. Taken out of the
    // intent once shown, so the list doesn't come back when Android restarts the home screen.
    // Home held down (HomeGuard) asks for the recently used apps the same way, with --es show recents.
    private void showRequested(Intent intent) {
        String show = intent == null ? null : intent.getStringExtra("show");
        if (!"apps".equals(show) && !"recents".equals(show)) return;
        intent.removeExtra("show");
        root.post("apps".equals(show) ? this::showAllApps : this::showRecents);
    }

    // The apps used last, newest first (the one just left on top), from usage stats; OK opens one.
    private void showRecents() {
        refreshRecents();
        final List<String> pkgs = new ArrayList<>(lastUsed.keySet());
        java.util.Collections.sort(pkgs, (a, b) -> Long.compare(lastUsed.get(b), lastUsed.get(a)));
        while (pkgs.size() > RECENTS) pkgs.remove(pkgs.size() - 1);
        if (pkgs.isEmpty()) return;
        final PackageManager pm = getPackageManager();
        List<String> labels = new ArrayList<>();
        for (String pkg : pkgs) {
            String label = pkg;
            try { label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); } catch (Exception ignored) {}
            labels.add(label);
        }
        panel(Strings.t("Recently used"), labels, pos -> fit(pm.getApplicationIcon(pkgs.get(pos)), px(48), px(48)),
                px(48), -1, null, which -> open(pkgs.get(which)));
    }

    // Saves an app list passed with --es apps "..." and shortcuts passed with --es quick "...".
    // Returns true if it changed.
    // When Android kills the home screen in the background, it can start it again with the same old
    // intent; the stamp install.sh sends stops that from undoing changes made on the TV since.
    private boolean saveConfig(Intent intent) {
        String apps = intent == null ? null : intent.getStringExtra("apps");
        String quick = intent == null ? null : intent.getStringExtra("quick");
        if (apps == null && quick == null) return false;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String stamp = intent.getStringExtra("stamp");
        if (stamp != null && stamp.equals(prefs.getString("stamp", null))) return false;
        SharedPreferences.Editor e = prefs.edit();
        if (stamp != null) e.putString("stamp", stamp);
        if (apps != null) {
            apps = apps.trim();
            if (apps.isEmpty() || apps.equalsIgnoreCase("auto")) e.remove("apps");
            else e.putString("apps", apps);
        }
        if (quick != null) {
            quick = quick.trim();
            if (quick.isEmpty() || quick.equalsIgnoreCase("none")) e.remove("quick");
            else e.putString("quick", quick);
        }
        e.apply();
        dirty = true;
        return true;
    }

    private List<String> appList(PackageManager pm) {
        List<String> out = new ArrayList<>();
        String saved = getSharedPreferences(PREFS, 0).getString("apps", null);
        String[] wanted = saved != null ? saved.split("[,\\s]+") : Apps.LIST;
        for (String p : wanted) if (!p.isEmpty() && !out.contains(p) && launchIntent(pm, p) != null) out.add(p);
        if (saved == null) {
            // Top up with other TV apps people installed (or updated), skipping launchers and system tools.
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                if (out.size() >= PER_ROW) break;
                String p = ri.activityInfo.packageName;
                int flags = ri.activityInfo.applicationInfo.flags;
                boolean stockSystem = (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                if (stockSystem || out.contains(p) || NOT_APPS.contains(p) || p.equals(getPackageName())) continue;
                out.add(p);
            }
        }
        return out;
    }

    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
        if (arranging && movingKey == null) stopArrange();
        root.removeCallbacks(statusTick);
        shelfKey = null; // so the shelf (and the preview) start again on the way back
        // Kept on disk: Android may stop this app while you're away, and cameFrom() needs it on the way back.
        getSharedPreferences(PREFS, 0).edit().putLong("awayAt", System.currentTimeMillis()).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keepHomeGuardOn();
        restartStatus();
        // Rebuild only when apps were installed, removed or updated.
        if (dirty) {
            dirty = false;
            buildTiles();
        } else {
            refreshRecents(); // coming back from an app: it's the most recent one now
        }
        refreshNetwork();
        // Like Apple TV, Home comes back on the app you just left (and the shelf says when that was), on
        // Live TV after Live TV, and on the first app after the TV was off.
        String back = movingKey == null ? cameFrom() : null;
        View target = back == null ? null : tile(back);
        if (target == null) target = getCurrentFocus();
        if (target == null && !tiles.isEmpty()) target = tiles.get(0);
        if (target != null) {
            shelfKey = null;
            if (target.isFocused() && target.getOnFocusChangeListener() != null)
                target.getOnFocusChangeListener().onFocusChange(target, true);
            else target.requestFocus();
        }
    }

    // Where Home should come back to, from what was in front since it was left (usage stats, allowed
    // over adb): that app's tile, Live TV's pill, or the first app when the TV was off or started in
    // between. Null keeps the focus where it was: an HDMI input (its pill is still focused when it was
    // opened from here), a menu of the TV's, or no usage stats.
    private String cameFrom() {
        long since = getSharedPreferences(PREFS, 0).getLong("awayAt", 0), now = System.currentTimeMillis();
        android.app.usage.UsageStatsManager um =
                (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (um == null || since <= 0 || since > now) return null;
        String last = null;
        boolean wasOff = false;
        try {
            android.app.usage.UsageEvents events = um.queryEvents(since, now);
            android.app.usage.UsageEvents.Event e = new android.app.usage.UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(e);
                int type = e.getEventType();
                // Screen on or off (15, 16), shut down or started (26, 27): what ran before that is old.
                if (type == 15 || type == 16 || type == 26 || type == 27) {
                    wasOff = true;
                    last = null;
                } else if (type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
                        && !getPackageName().equals(e.getPackageName())) {
                    last = e.getPackageName();
                }
            }
        } catch (Exception ex) {
            return null;
        }
        if (last != null && rowApps.contains(last)) return last;
        if (last != null && last.equals(livePackage())) return LIVE;
        // HDMI: which input played isn't told to apps. The focused pill if it is one (it was opened from
        // here), else the one input with a device on, if that's clear.
        if (INPUT_PLAYER.equals(last)) {
            View f = getCurrentFocus();
            if (f != null && f.getTag() instanceof String && isInput((String) f.getTag())) return null;
            if (cecDevices.size() == 1) return cecDevices.keySet().iterator().next(); // keyed by HDMI port
        }
        // The TV turned on into a settings screen, say: start at the first app like a fresh start.
        if (wasOff) return rowApps.isEmpty() ? null : rowApps.get(0);
        return null;
    }

    // Android drops HomeGuard from the accessibility services whenever this app is stopped (an update,
    // a force stop), and then Home, Settings and the other buttons quietly stop working. With
    // WRITE_SECURE_SETTINGS (granted over adb) the home screen puts it back each time it comes up,
    // unless homeguard.sh turned it off on purpose (it leaves tvhome_homeguard=off for that).
    private void keepHomeGuardOn() {
        if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != PackageManager.PERMISSION_GRANTED) return;
        android.content.ContentResolver cr = getContentResolver();
        if ("off".equals(Settings.Secure.getString(cr, "tvhome_homeguard"))) return;
        String me = getPackageName() + "/.HomeGuard"; // the short form homeguard.sh and check.sh use
        String now = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String list = ":" + now + ":";
        if (now != null && (list.contains(":" + me + ":")
                || list.contains(":" + getPackageName() + "/" + HomeGuard.class.getName() + ":"))) return;
        try {
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    now == null || now.isEmpty() ? me : now + ":" + me);
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (SecurityException ignored) {}
    }

    // When each app was last open, from Android's usage stats (allowed with:
    // adb shell appops set com.ansub.tvhome GET_USAGE_STATS allow). Without that permission nothing is
    // known, and the shelf just shows the app's name.
    private void refreshRecents() {
        lastUsed.clear();
        android.app.usage.UsageStatsManager um =
                (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (um == null) return;
        long now = System.currentTimeMillis();
        List<android.app.usage.UsageStats> stats;
        try {
            stats = um.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_BEST, now - 14L * 24 * 3600 * 1000, now);
        } catch (Exception e) {
            return;
        }
        if (stats == null) return;
        PackageManager pm = getPackageManager();
        for (android.app.usage.UsageStats s : stats) {
            String p = s.getPackageName();
            long t = android.os.Build.VERSION.SDK_INT >= 29 ? s.getLastTimeVisible() : s.getLastTimeUsed();
            if (t <= 0 || t > now || p.equals(getPackageName()) || NOT_APPS.contains(p)) continue;
            Long had = lastUsed.get(p);
            if (had != null && had >= t) continue;
            if (had == null && launchIntent(pm, p) == null) continue;
            lastUsed.put(p, t);
        }
    }

    // What the shelf says under a tile's name: when an app was last open, what an input or Settings
    // is, or nothing. Only "last opened", never "continue watching": the home screen can't see what
    // was playing in the app.
    private String subFor(String key) {
        if (isInput(key)) {
            String device = cecDevices.get(key);
            if (device != null && !device.isEmpty())
                return device + "  ·  " + Strings.t(key.equals(deviceJustOn) ? "is on, press OK to watch" : "HDMI input");
            Boolean picture = signal.get(key);
            return Strings.t("HDMI input") + "  ·  "
                    + Strings.t(picture == null ? "No device on" : picture ? "Picture" : "No signal");
        }
        if (SETTINGS.equals(key)) return android.text.TextUtils.join("  ·  ", translated(SETTINGS_ITEMS));
        String pkg = LIVE.equals(key) ? livePackage() : key;
        Long t = pkg == null ? null : lastUsed.get(pkg);
        if (t == null) return "";
        long now = System.currentTimeMillis();
        // Android says "0 minutes ago" for the last minute.
        CharSequence ago = now - t < 60_000 ? Strings.t("just now")
                : android.text.format.DateUtils.getRelativeTimeSpanString(t, now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
        return Strings.t("Last opened") + "  ·  " + ago;
    }

    private Drawable liveTvBanner() {
        Intent i = liveTvIntent();
        if (i == null) return null;
        PackageManager pm = getPackageManager();
        try {
            Drawable b = pm.getActivityBanner(i);
            if (b == null && i.getComponent() != null) b = pm.getApplicationBanner(i.getComponent().getPackageName());
            return toned(b);
        } catch (Exception e) {
            return null;
        }
    }

    // The preview found out whether an input sends a picture: update its dot and the shelf's line.
    // Only redraws when that changed, and only while that input is still the one in focus.
    // The HDMI preview has a picture (or lost it): the wallpaper fades out to let it through, dimmed.
    // Also called again when the picture's resolution becomes known, to fill in the LIVE badge.
    private void onPicture(boolean showing) {
        wallpaperView.animate().alpha(showing ? 0f : 1f).setDuration(350).start();
        dimView.animate().alpha(showing ? 1f : 0f).setDuration(350).start();
        dockShade.animate().alpha(showing ? 1f : 0f).setDuration(350).start();
        if (showing) {
            String info = preview.videoInfo();
            liveText.setText(info == null ? "LIVE" : "LIVE  ·  " + info);
        }
        liveBadge.animate().alpha(showing ? 1f : 0f).setDuration(350).start();
    }

    private void onSignal(String inputId, boolean picture) {
        if (Boolean.valueOf(picture).equals(signal.get(inputId))) return;
        signal.put(inputId, picture);
        if (!inputId.equals(focusedKey)) return;
        shelfSub.setText(subFor(inputId));
        shelfSub.setVisibility(View.VISIBLE);
        View pill = tile(inputId);
        boolean dot = cecDevices.containsKey(inputId) || picture;
        if (pill != null && dot != (((ViewGroup) pill).getChildCount() > 2)) {
            dirty = true; // the dot comes and goes with a rebuild; focus and the preview stay put
            refreshApps();
        }
    }

    private String livePackage() {
        Intent i = liveTvIntent();
        return i == null || i.getComponent() == null ? null : i.getComponent().getPackageName();
    }

    // Back from a menu or a window on top (like the uninstall prompt): catch up on apps that changed.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) refreshApps();
        else okDownKey = null; // an OK still held finishes in the window that took over
    }

    // Rebuilds the tiles if apps changed, but only while Home is in front with no menu open and no
    // app being moved; otherwise the next onResume, focus change or end of the move does it.
    private void refreshApps() {
        if (!dirty || !hasWindowFocus() || movingKey != null) return;
        dirty = false;
        buildTiles();
    }

    // The TV's language changed: start over so the labels and the date follow it.
    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        if (!java.util.Locale.getDefault().equals(shownLocale)) recreate();
    }

    @Override
    public void onBackPressed() { /* home screen: nothing to go back to */ }

    // Static gradient, drawn once by the window: no extra view, no animation.
    private Drawable background(int[] c) {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {c[0], c[1], c[2]});
        GradientDrawable top = radial(c[3], 0.08f, -0.12f, px(1250));
        GradientDrawable bottom = radial(c[4], 1.0f, 1.1f, px(1000));
        for (GradientDrawable g : new GradientDrawable[] {base, top, bottom}) g.setDither(true);
        LayerDrawable wallpaper = new LayerDrawable(new Drawable[] {base, top, bottom});
        // Opaque, as it is (the base gradient is solid): left to itself it reports "translucent" for its
        // soft glows, and once the HDMI preview shows a picture, this TV then leaves the whole
        // background out as see-through, black.
        wallpaper.setOpacity(android.graphics.PixelFormat.OPAQUE);
        return wallpaper;
    }

    // The saved wallpaper, as an index into WALLPAPERS (Graphite, the most Apple-like, if none is saved).
    private int wallpaper() {
        String saved = getSharedPreferences(PREFS, 0).getString("wallpaper", null);
        int i = Arrays.asList(WALLPAPER_NAMES).indexOf(saved);
        return i >= 0 ? i : Arrays.asList(WALLPAPER_NAMES).indexOf("Graphite");
    }

    private void showWallpaper(int i) {
        wallpaperView.setBackground(baked(background(WALLPAPERS[i])));
    }

    // The wallpaper's gradients, and a light shade at the top for the clock, drawn once into a
    // quarter-size bitmap, shown stretched: the same look, but the GPU just copies a picture instead of
    // working out four full-screen gradients (two of them radial) for every frame, which made moving
    // through the tiles stutter on this TV. (The shelf's wash carries its own shade, see shelfWash.)
    private Drawable baked(Drawable d) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels, h = dm.heightPixels;
        try {
            Bitmap b = Bitmap.createBitmap(w / 4, h / 4, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.scale(0.25f, 0.25f);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            GradientDrawable top = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {0x59000000, 0x00000000, 0x00000000, 0x00000000});
            top.setBounds(0, 0, w, px(SHELF_H));
            top.draw(c);
            BitmapDrawable out = new BitmapDrawable(getResources(), b);
            out.setFilterBitmap(true);
            return out;
        } catch (Throwable t) {
            return d;
        }
    }

    // Shows a tile on the top shelf: a wash of its colours, its art sharp at the right, and its name.
    // Inputs and settings have no art, so the shelf clears to the wallpaper. Old art cross-fades out.
    private void showOnShelf(String key, String title, String sub, Drawable art) {
        if (shelf == null || key.equals(shelfKey)) return;
        shelfKey = key;
        if (!key.equals(deviceJustOn)) deviceJustOn = null;
        preview.preview(isInput(key) ? key : null);
        Drawable[] made = art == null ? null : shelfFor(key, art);
        crossFade(shelf, made == null ? null : made[0]);
        crossFade(shelfArt, made == null ? null : made[1]);
        shelfArt.animate().alpha(art == null ? 0f : 1f).setDuration(260).start();
        if (art != null) {
            shelfArt.setScaleX(0.97f);
            shelfArt.setScaleY(0.97f);
            shelfArt.animate().scaleX(1f).scaleY(1f).setDuration(320)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        shelfTitle.setText(title);
        shelfSub.setText(sub);
        shelfSub.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
        for (View v : new View[] {shelfTitle, shelfSub}) {
            v.setAlpha(0f);
            v.setTranslationX(px(-16));
            v.animate().alpha(1f).translationX(0).setDuration(260)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    // The wallpapers, each with a small dot of its colours. Moving through the list shows each one
    // behind the panel; OK keeps it, Back goes back to the one before.
    private void showWallpapers() {
        final int size = px(40);
        panel(Strings.t("Wallpaper"), Arrays.asList(WALLPAPER_NAMES), pos -> {
            int[] c = WALLPAPERS[pos];
            GradientDrawable dot = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {glow(c[3], c[0]), c[1], glow(c[4], c[0])});
            dot.setShape(GradientDrawable.OVAL);
            dot.setStroke(Math.max(1, px(2)), 0x26FFFFFF);
            dot.setSize(size, size);
            return dot;
        }, size, wallpaper(), this::showWallpaper, which -> {
            getSharedPreferences(PREFS, 0).edit().putString("wallpaper", WALLPAPER_NAMES[which]).apply();
            showWallpaper(which);
        });
    }

    // The look picked under Tiles.
    private int tileTone() {
        int i = Arrays.asList(TILE_TONES).indexOf(getSharedPreferences(PREFS, 0).getString("tiles", null));
        return i >= 0 ? i : TONE_DARK; // dark, like the rest of the page
    }

    // The look the tiles have right now: Auto is Light by day and Dark in the evening and at night.
    private int shownTone() {
        int tone = tileTone();
        if (tone != TONE_AUTO) return tone;
        return isNight() ? TONE_DARK : TONE_LIGHT;
    }

    private static boolean isNight() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        return hour < DAY_FROM || hour >= DAY_UNTIL;
    }

    // Auto at night also takes the wallpaper down a little, over its picture (so it fades with it for HDMI).
    private void applyNight() {
        boolean night = tileTone() == TONE_AUTO && isNight();
        wallpaperView.setForeground(night ? new ColorDrawable(0x59000000) : null);
    }

    private int builtTone = -1; // the look the tiles were last drawn in

    // Auto turning from day to night (or back) while the home screen is up: redraw in the other look.
    private void followDayAndNight() {
        if (builtTone == -1 || shownTone() == builtTone) return;
        shelfCache.clear();
        shelfKey = null;
        buildTiles();
    }

    // The looks, each with a small swatch, ticked when chosen; under them, whether coloured backgrounds
    // (Prime's blue) change too. OK redraws the tiles at once, behind the open panel.
    private void showTiles() {
        final int size = px(40);
        List<String> rows = new ArrayList<>(Arrays.asList(translated(TILE_TONES)));
        rows.add(Strings.t("Brand colours too"));
        final int brandRow = TILE_TONES.length;
        final boolean[] on = new boolean[rows.size()];
        on[tileTone()] = true;
        on[brandRow] = brandColoursToo();
        panel(Strings.t("Tiles"), rows, pos -> {
            GradientDrawable swatch;
            if (pos == brandRow) swatch = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {0xFF0A7BFF, 0xFF0A7BFF, 0xFF0B0C10, 0xFF0B0C10});
            else if (pos == TONE_ORIGINAL) swatch = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {0xFFE50914, Color.WHITE, 0xFF0A7BFF});
            else if (pos == TONE_AUTO) swatch = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[] {0xFFF2F2F2, 0xFFF2F2F2, 0xFF0B0C10, 0xFF0B0C10});
            else if (pos == TONE_MONO) swatch = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {0xFFD0D0D0, 0xFF0B0C10});
            else if (pos == TONE_SPOTLIGHT) {
                swatch = radial(Color.WHITE, 0.5f, 0.5f, size / 2f);
                swatch.setColors(new int[] {Color.WHITE, 0xFF0B0C10});
            } else {
                swatch = new GradientDrawable();
                swatch.setColor(pos == TONE_LIGHT ? 0xFFF2F2F2 : pos == TONE_GLASS ? Style.GLASS : 0xFF0B0C10);
            }
            swatch.setCornerRadius(px(10));
            swatch.setStroke(Math.max(1, px(2)), Style.GLASS_STROKE);
            swatch.setSize(size, size);
            return swatch;
        }, size, -1, null, on, which -> {
            if (which == brandRow) {
                getSharedPreferences(PREFS, 0).edit().putBoolean("tilesBrand", on[brandRow]).apply();
            } else {
                for (int i = 0; i < brandRow; i++) on[i] = i == which; // one look at a time
                getSharedPreferences(PREFS, 0).edit().putString("tiles", TILE_TONES[which]).apply();
            }
            shelfCache.clear();
            shelfKey = null;
            buildTiles();
        });
    }

    // One look for all tiles: a banner on a white, grey or black background gets its lightness flipped
    // when that background is the wrong way round for the tone, with its colours kept: white goes black,
    // black lettering goes white, Netflix's red stays red. A coloured background (Prime's blue) is the
    // app's own colour: it stays, unless "Brand colours too" is on. Done once per banner, at most 640x360,
    // when the tiles are built (so apps installed later get the look as well).
    private Drawable toned(Drawable d) {
        int tone = shownTone();
        if (d == null || tone == TONE_ORIGINAL) return d;
        int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
        if (iw <= 0 || ih <= 0) return d;
        float s = Math.min(1f, Math.min(640f / iw, 360f / ih));
        int w = Math.max(1, Math.round(iw * s)), h = Math.max(1, Math.round(ih * s));
        Drawable fitted = fit(d, w, h);
        if (!(fitted instanceof BitmapDrawable)) return d;
        Bitmap b = ((BitmapDrawable) fitted).getBitmap();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);

        // The background: the middle value of the edge pixels, channel by channel, skipping see-through ones.
        int[][] edge = new int[3][2 * (w + h)];
        int n = 0;
        for (int i = 0; i < w * h; i++) {
            int x = i % w, y = i / w;
            if (x != 0 && y != 0 && x != w - 1 && y != h - 1) continue;
            int c = px[i];
            if (n == edge[0].length || (c >>> 24) < 128) continue;
            edge[0][n] = (c >> 16) & 0xFF;
            edge[1][n] = (c >> 8) & 0xFF;
            edge[2][n] = c & 0xFF;
            n++;
        }
        if (n < w) return d; // mostly see-through: nothing to flip
        int[] bg = new int[3];
        for (int ch = 0; ch < 3; ch++) {
            int[] v = Arrays.copyOf(edge[ch], n);
            Arrays.sort(v);
            bg[ch] = v[n / 2];
        }
        int max = Math.max(bg[0], Math.max(bg[1], bg[2])), min = Math.min(bg[0], Math.min(bg[1], bg[2]));
        boolean coloured = max - min > 40; // a brand's own colour, like Prime's blue
        if (coloured && !brandColoursToo()) return fitted;
        int back = Color.rgb(bg[0], bg[1], bg[2]);
        boolean flip = !coloured && (max + min > 255) != (tone == TONE_LIGHT); // light but dark wanted, or the other way
        if (flip) {
            for (int i = 0; i < px.length; i++) px[i] = flipLightness(px[i]);
            back = flipLightness(back);
        }
        if (!coloured && !flip && tone != TONE_GLASS) return fitted;
        // Glass, or a coloured background going: the background goes see-through, leaving the logo. Edge
        // pixels, part logo and part background, keep just the logo's part, so the edges stay smooth.
        if (coloured || tone == TONE_GLASS) keyOut(px, back);
        if (coloured) {
            // The logo was made for its colour: white on Prime's blue stays white on dark, and turns
            // dark on light. Then it goes onto the tone's own background (glass stays see-through).
            boolean wantLight = tone == TONE_LIGHT;
            double sum = 0, weight = 0;
            for (int c : px) {
                int a = c >>> 24;
                int mx = Math.max((c >> 16) & 0xFF, Math.max((c >> 8) & 0xFF, c & 0xFF));
                int mn = Math.min((c >> 16) & 0xFF, Math.min((c >> 8) & 0xFF, c & 0xFF));
                sum += a * (mx + mn) / 510.0;
                weight += a;
            }
            boolean logoLight = weight > 0 && sum / weight > 0.5;
            int under = wantLight ? 0xFFF4F4F4 : 0xFF0B0B0D;
            for (int i = 0; i < px.length; i++) {
                int c = logoLight == wantLight ? flipLightness(px[i]) : px[i];
                px[i] = tone == TONE_GLASS ? c : over(c, under);
            }
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
        return fitted;
    }

    // Makes the background colour see-through: by how far each pixel is from it, with the colour of the
    // part that isn't background put back, so anti-aliased edges keep their colour.
    private static void keyOut(int[] px, int back) {
        int br = Color.red(back), bgg = Color.green(back), bb = Color.blue(back);
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, bl = c & 0xFF;
            int dist = Math.max(Math.abs(r - br), Math.max(Math.abs(g - bgg), Math.abs(bl - bb)));
            float a = Math.min(1f, dist / 48f);
            if (a >= 1f) continue;
            if (a <= 0f) { px[i] = 0; continue; }
            r = Math.max(0, Math.min(255, Math.round(br + (r - br) / a)));
            g = Math.max(0, Math.min(255, Math.round(bgg + (g - bgg) / a)));
            bl = Math.max(0, Math.min(255, Math.round(bb + (bl - bb) / a)));
            px[i] = (Math.round((c >>> 24) * a) << 24) | (r << 16) | (g << 8) | bl;
        }
    }

    // c drawn over an opaque colour.
    private static int over(int c, int under) {
        int a = c >>> 24;
        int r = (((c >> 16) & 0xFF) * a + ((under >> 16) & 0xFF) * (255 - a)) / 255;
        int g = (((c >> 8) & 0xFF) * a + ((under >> 8) & 0xFF) * (255 - a)) / 255;
        int b = ((c & 0xFF) * a + (under & 0xFF) * (255 - a)) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private boolean brandColoursToo() {
        return getSharedPreferences(PREFS, 0).getBoolean("tilesBrand", false);
    }

    // Lightness to 1 - lightness, hue and colour strength kept: white and black swap, red stays red.
    private static int flipLightness(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        int shift = 255 - Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
        r = Math.max(0, Math.min(255, r + shift));
        g = Math.max(0, Math.min(255, g + shift));
        b = Math.max(0, Math.min(255, b + shift));
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    // Black & white and Spotlight: how a banner looks while it isn't focused (null: as it is).
    private android.graphics.ColorFilter restFilter() {
        int tone = shownTone();
        android.graphics.ColorMatrix m = new android.graphics.ColorMatrix();
        if (tone == TONE_MONO) m.setSaturation(0f);
        else if (tone == TONE_SPOTLIGHT) m.setScale(0.45f, 0.45f, 0.45f, 1f);
        else return null;
        return new android.graphics.ColorMatrixColorFilter(m);
    }

    // A glow colour made solid over the base, three times as strong, so a small dot shows it.
    private static int glow(int glow, int base) {
        float a = Math.min(1f, 3 * (glow >>> 24) / 255f);
        int r = Math.round(Color.red(base) + (Color.red(glow) - Color.red(base)) * a);
        int g = Math.round(Color.green(base) + (Color.green(glow) - Color.green(base)) * a);
        int b = Math.round(Color.blue(base) + (Color.blue(glow) - Color.blue(base)) * a);
        return Color.rgb(r, g, b);
    }

    private static GradientDrawable radial(int color, float cx, float cy, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[] {color, color & 0x00FFFFFF});
        g.setGradientCenter(cx, cy);
        g.setGradientRadius(r);
        return g;
    }

    private void buildTiles() {
        // While tiles are swapped out, removing the focused one would hand focus down the row (and
        // overwrite focusedKey), so hold focus off until the new tiles are in.
        String want = focusedKey;
        int wantAt = rowApps.indexOf(want); // where it was, in case the app is gone now
        root.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        for (View t : tiles) {
            View wrap = (View) t.getParent();
            ((ViewGroup) wrap.getParent()).removeView(wrap);
        }
        tiles.clear();
        rowApps.clear();
        movingKey = null;
        showHint(); // still there while arranging

        builtTone = shownTone();
        applyNight();
        boolean glassLook = builtTone == TONE_GLASS;
        artShape.setColor(glassLook ? Style.GLASS : 0xFF1E1A1C);
        artShape.setStroke(glassLook ? Math.max(1, px(Style.STROKE)) : 0, Style.GLASS_STROKE);

        PackageManager pm = getPackageManager();
        int w = px(TILE_W), h = px(TILE_H);
        tileW = w;
        tileH = h;

        List<String> apps = appList(pm);
        // The apps in one row under the top shelf (scrolling sideways past six), the inputs as pills
        // under them, all on the glass dock.
        gridTop = px(ROW1_TOP);
        int pillTop = gridTop + h + px(PILL_ROOM);
        // The row's window: inside the dock's edges, tall enough for a focused tile to grow and for
        // its name under it.
        int sw = getResources().getDisplayMetrics().widthPixels;
        // Just wider than six tiles, by the room a focused tile grows into: apps further along stay out
        // of sight (no half app at the edge), and the dock's margin beside it is free for the arrows.
        stripX = px(TILE_LEFT - ROW_GROW);
        stripY = gridTop - px(30);
        appScroll.setLayoutParams(at(stripX, stripY, sw - 2 * stripX, h + px(30) + px(PILL_ROOM - 10)));
        appStrip.setPadding(0, 0, px(TILE_LEFT) - stripX, 0); // the last app ends as far in as the first starts
        appScroll.scrollTo(0, 0);
        int arrowY = gridTop + h / 2 - px(ARROW) / 2;
        moreLeft.setLayoutParams(at(px(DOCK_X) + (stripX - px(DOCK_X) - px(ARROW)) / 2, arrowY, px(ARROW), px(ARROW)));
        moreRight.setLayoutParams(at(sw - stripX + (stripX - px(DOCK_X) - px(ARROW)) / 2, arrowY, px(ARROW), px(ARROW)));
        appScroll.post(() -> showArrows(0));
        int dockBottom = pillTop + px(PILL_H) + px(DOCK_PAD);
        ((FrameLayout.LayoutParams) moveHint.getLayoutParams()).topMargin = px(DOCK_TOP) - px(48);
        root.setMinimumHeight(dockBottom + px(DOCK_X)); // as far from the bottom as from the sides
        dock.setLayoutParams(at(px(DOCK_X), px(DOCK_TOP), getResources().getDisplayMetrics().widthPixels - 2 * px(DOCK_X),
                dockBottom - px(DOCK_TOP)));
        dockShade.setLayoutParams(at(px(DOCK_X), px(DOCK_TOP), getResources().getDisplayMetrics().widthPixels - 2 * px(DOCK_X),
                dockBottom - px(DOCK_TOP)));

        for (String pkg : apps) {
            Intent li = launchIntent(pm, pkg);
            if (li == null) continue;
            Drawable banner = null;
            try {
                banner = pm.getActivityBanner(li);
                if (banner == null) banner = pm.getApplicationBanner(pkg);
            } catch (Exception ignored) {}
            String label = pkg;
            try { label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); } catch (Exception ignored) {}
            // Apps without a TV banner get a plain tile with their normal icon.
            Drawable icon = null;
            if (banner == null) try { icon = pm.getApplicationIcon(pkg); } catch (Exception ignored) {}
            banner = toned(banner);
            int i = rowApps.size();
            rowApps.add(pkg);
            addTile(appStrip, pkg, label, banner, null, icon, gridX(i), gridY(i), w, h);
        }

        // Under the apps, as small glass pills so they weigh less than the apps: Live TV (or the Play
        // Store on a TV without it), the HDMI inputs, Settings and Add app.
        int x = px(TILE_LEFT);
        boolean live = liveTvIntent() != null;
        boolean store = !live && !apps.contains(PLAY_STORE) && launchIntent(pm, PLAY_STORE) != null;
        if (live) x += addPill(LIVE, Strings.t("Live TV"), LineIcon.LIVE, x, pillTop, false) + px(PILL_GAP);
        if (store) x += addPill(PLAY_STORE, "Play Store", LineIcon.PLAY, x, pillTop, false) + px(PILL_GAP);
        refreshCecDevices();
        for (TvInputInfo in : hdmiInputs())
            x += addPill(in.getId(), inputLabel(in), LineIcon.HDMI, x, pillTop,
                    cecDevices.containsKey(in.getId()) || Boolean.TRUE.equals(signal.get(in.getId()))) + px(PILL_GAP);
        x += addPill(SETTINGS, Strings.t("Settings"), LineIcon.SETTINGS, x, pillTop, false) + px(PILL_GAP);
        addPill(ADD, Strings.t("Add app"), LineIcon.PLUS, x, pillTop, false);

        // The network icon left of the clock: Wi-Fi, Ethernet or offline (see refreshNetwork).
        // Added last, so the first app keeps first focus.
        netIcon = addIcon(NETWORK, Strings.t("Network"), null);
        netState = -1;
        refreshNetwork();
        placeTopRight();

        refreshRecents();
        root.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // An app that was uninstalled hands focus to the one now in its place, as Remove does.
        if (wantAt >= 0 && !rowApps.contains(want))
            want = rowApps.isEmpty() ? ADD : rowApps.get(Math.min(wantAt, rowApps.size() - 1));
        View restore = null;
        for (View t : tiles) if (t.getTag().equals(want)) restore = t;
        if (restore != null) restore.requestFocus();
    }

    private void addTile(ViewGroup parent, final String key, String label, Drawable banner, int[] lineIcon, Drawable appIcon,
                         int x, int y, int w, int h) {
        FrameLayout card = new FrameLayout(this);
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        final TextView name; // glass tiles only: label and line icon turn dark on the white focus
        ImageView bannerView = null;
        final android.graphics.ColorFilter rest = restFilter();
        final ImageView glyph; // the line icon's view; tinted on the view, so it redraws at once
        final LineIcon line = lineIcon != null ? new LineIcon(lineIcon) : null;
        if (banner != null) {
            name = null;
            glyph = null;
            if (shownTone() == TONE_GLASS) {
                bg.setColor(Style.GLASS);
                bg.setStroke(Math.max(1, px(2)), Style.GLASS_STROKE);
            } else {
                bg.setColor(0xFF1E1A1C);
            }
            bannerView = new ImageView(this);
            bannerView.setImageDrawable(fit(banner, w, h));
            bannerView.setColorFilter(rest);
            card.addView(bannerView, new FrameLayout.LayoutParams(-1, -1));
        } else {
            // Frosted "glass" tile: icon above label. Focused, it turns white like tvOS buttons.
            bg.setColor(Style.GLASS);
            bg.setStroke(Math.max(1, px(2)), Style.GLASS_STROKE);
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            ImageView iv = new ImageView(this);
            int size = lineIcon != null ? px(44) : px(64);
            glyph = line != null ? iv : null;
            if (line != null) iv.setImageDrawable(line);
            else if (appIcon != null) iv.setImageDrawable(fit(appIcon, size, size));
            inner.addView(iv, new LinearLayout.LayoutParams(size, size));
            name = text(px(Style.LABEL), Style.INK, semibold);
            name.setText(label);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setGravity(Gravity.CENTER);
            name.setPadding(px(16), 0, px(16), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = px(12);
            inner.addView(name, lp);
            card.addView(inner, new FrameLayout.LayoutParams(-1, -1));
        }
        // Light catching the glass: a soft gloss on the top half, and a sheen that sweeps across once
        // when the tile gets focus. Both are clipped to the tile's rounded corners.
        final View gloss = new View(this);
        gloss.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x38FFFFFF, 0x0AFFFFFF, 0x00FFFFFF}));
        gloss.setAlpha(0f);
        card.addView(gloss, new FrameLayout.LayoutParams(-1, h / 2));
        final View sheen = new View(this);
        sheen.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0x00FFFFFF, Style.GLASS_STROKE, 0x00FFFFFF}));
        sheen.setRotation(18f);
        sheen.setVisibility(View.INVISIBLE);
        card.addView(sheen, new FrameLayout.LayoutParams(w / 3, h * 2, Gravity.CENTER_VERTICAL));
        card.setBackground(bg);
        card.setClipToOutline(true);
        card.setFocusable(true);
        card.setTag(key);

        final Drawable art = banner != null ? banner : appIcon;

        // Banner tiles show their name underneath when focused.
        final TextView caption = banner == null ? null : text(px(Style.LABEL), Color.WHITE, semibold);
        if (caption != null) {
            caption.setText(label);
            caption.setGravity(Gravity.CENTER_HORIZONTAL);
            caption.setVisibility(View.INVISIBLE);
        }

        final boolean glass = banner == null;
        final ImageView bannerArt = bannerView;
        card.setOnFocusChangeListener((v, has) -> {
            if (bannerArt != null && rest != null) bannerArt.setColorFilter(has ? null : rest);
            if (has) {
                focusedKey = key;
                showOnShelf(key, label, subFor(key), art); // asked now: "5 min ago" keeps changing
                if (parent == appStrip) scrollRowTo(rowApps.indexOf(key));
            }
            if (glass) {
                bg.setColor(has ? Style.FOCUS : Style.GLASS);
                if (name != null) name.setTextColor(has ? Style.FOCUS_INK : Style.INK);
                if (glyph != null) {
                    if (has) glyph.setColorFilter(new PorterDuffColorFilter(Style.FOCUS_INK, PorterDuff.Mode.SRC_IN));
                    else glyph.clearColorFilter();
                }
            }
            // Lift with a little spring, settle back gently: the tvOS focus feel.
            v.animate().scaleX(has ? FOCUS_SCALE : 1f).scaleY(has ? FOCUS_SCALE : 1f)
                    .setDuration(has ? 260 : 180)
                    .setInterpolator(has ? new OvershootInterpolator(1.6f) : new DecelerateInterpolator())
                    .withLayer().start();
            gloss.animate().alpha(has ? 1f : 0f).setDuration(200).start();
            sheen.animate().cancel();
            if (has) {
                sheen.setVisibility(View.VISIBLE);
                sheen.setTranslationX(-w / 2f);
                sheen.animate().translationX(w * 1.2f).setDuration(700).setStartDelay(80)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> sheen.setVisibility(View.INVISIBLE)).start();
            } else {
                sheen.setVisibility(View.INVISIBLE);
            }
            if (caption != null) caption.setVisibility(has ? View.VISIBLE : View.INVISIBLE);
        });
        final boolean editable = rowApps.contains(key);
        card.setOnClickListener(v -> open(key));
        if (editable) card.setOnLongClickListener(v -> { showEditMenu(key); return true; });
        card.setOnKeyListener((v, code, ev) -> {
            boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
            boolean down = ev.getAction() == KeyEvent.ACTION_DOWN;
            if (key.equals(movingKey)) return moveKey(key, code, ok, down);
            // Right on the last app goes round to the first, Left on the first to the last.
            if (editable && code == KeyEvent.KEYCODE_DPAD_RIGHT && rowApps.size() > 1
                    && key.equals(rowApps.get(rowApps.size() - 1))) {
                if (down) tile(rowApps.get(0)).requestFocus();
                return true;
            }
            if (editable && code == KeyEvent.KEYCODE_DPAD_LEFT && rowApps.size() > 1 && key.equals(rowApps.get(0))) {
                if (down) tile(rowApps.get(rowApps.size() - 1)).requestFocus();
                return true;
            }
            // Arranging: OK picks this app up (then moveKey has it), Back ends arranging.
            // Only an OK that went down on this tile counts: the key-up of the OK that chose "Arrange apps"
            // lands here too, and must not pick up the first app.
            if (arranging && editable && (ok || code == KeyEvent.KEYCODE_BACK)) {
                if (down) {
                    if (ok && ev.getRepeatCount() == 0) okDownKey = key;
                } else if (!ok) {
                    stopArrange();
                } else {
                    boolean pressed = key.equals(okDownKey) && !ev.isCanceled();
                    okDownKey = null;
                    if (pressed) startMove(key);
                }
                return true;
            }
            if (editable && code == KeyEvent.KEYCODE_MENU) {
                if (!down) showEditMenu(key);
                return true;
            }
            if (!ok) return false;
            if (down) {
                if (ev.getRepeatCount() == 0) {
                    longPressed = false;
                    okDownKey = key;
                } else if (editable && !longPressed && (ev.isLongPress()
                        || ev.getEventTime() - ev.getDownTime() >= ViewConfiguration.getLongPressTimeout())) {
                    longPressed = true;
                    showEditMenu(key);
                }
            } else {
                // Only the key-up of an OK that went down on this tile opens the app, and not a cancelled
                // one (focus left while OK was held). View does the same for a click with isPressed().
                boolean pressed = key.equals(okDownKey) && !ev.isCanceled();
                okDownKey = null;
                if (pressed && !longPressed) open(key);
            }
            return true;
        });

        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(card, new FrameLayout.LayoutParams(w, h));
        if (caption != null) {
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(w, -2);
            cp.topMargin = h + px(22);
            wrap.addView(caption, cp);
        }
        parent.addView(wrap, at(x, y, w, -2));
        tiles.add(card);
    }

    // An input or system shortcut under the apps: a glass pill with a line icon and its name, white
    // when focused, like tvOS buttons. signal adds a green dot: an HDMI-CEC device on that input is on.
    // Returns its width, so the next one can follow.
    private int addPill(final String key, final String label, int[] lineIcon, int x, int y, boolean signal) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(px(PILL_PAD), 0, px(PILL_PAD), 0);
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(PILL_H) / 2f);
        bg.setColor(Style.GLASS);
        bg.setStroke(Math.max(1, px(2)), Style.GLASS_STROKE);
        pill.setBackground(bg);
        final ImageView iv = new ImageView(this);
        iv.setImageDrawable(new LineIcon(lineIcon));
        pill.addView(iv, new LinearLayout.LayoutParams(px(PILL_ICON), px(PILL_ICON)));
        final TextView t = text(px(Style.LABEL), Color.WHITE, semibold);
        t.setText(label);
        t.setSingleLine(true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.leftMargin = px(12);
        pill.addView(t, tl);
        int w = 2 * px(PILL_PAD) + px(PILL_ICON) + px(12) + Math.round(t.getPaint().measureText(label)) + 1;
        if (signal) {
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(0xFF34C759); // Apple's green
            dot.setBackground(g);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(px(12), px(12));
            dl.leftMargin = px(12);
            pill.addView(dot, dl);
            w += px(24);
        }
        // Live TV is an app too: the shelf shows its banner, like the app tiles do.
        final Drawable art = LIVE.equals(key) ? liveTvBanner() : null;

        pill.setFocusable(true);
        pill.setTag(key);
        pill.setContentDescription(label);
        pill.setOnFocusChangeListener((v, has) -> {
            if (has) {
                focusedKey = key;
                showOnShelf(key, label, subFor(key), art);
                if (arranging) stopArrange(); // left the app row
            }
            bg.setColor(has ? Color.WHITE : Style.GLASS);
            bg.setStroke(Math.max(1, px(2)), has ? Color.WHITE : Style.GLASS_STROKE);
            t.setTextColor(has ? Style.FOCUS_INK : Color.WHITE);
            if (has) iv.setColorFilter(new PorterDuffColorFilter(Style.FOCUS_INK, PorterDuff.Mode.SRC_IN));
            else iv.clearColorFilter();
            v.animate().scaleX(has ? Style.SCALE_SMALL : 1f).scaleY(has ? Style.SCALE_SMALL : 1f).setDuration(150)
                    .setInterpolator(new DecelerateInterpolator()).withLayer().start();
        });
        pill.setOnClickListener(v -> open(key));

        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(pill, new FrameLayout.LayoutParams(w, px(PILL_H)));
        root.addView(wrap, at(x, y, w, px(PILL_H)));
        tiles.add(pill);
        return w;
    }

    // An icon on its own, no tile or label: a glass circle shows behind it only while it's focused.
    // A round icon button in the top right row, between the download speed and the clock. The icon is as
    // tall as the text beside it, and as dim; focused, the white circle shows round it.
    private ImageView addIcon(final String key, String label, Drawable icon) {
        int b = px(ICON_BUTTON), size = px(STATUS_ICON);
        FrameLayout card = new FrameLayout(this);
        final GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.TRANSPARENT);
        card.setBackground(bg);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        card.addView(iv, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        card.setFocusable(true);
        card.setTag(key);
        card.setContentDescription(label);
        card.setOnFocusChangeListener((v, has) -> {
            if (has) focusedKey = key;
            bg.setColor(has ? Style.FOCUS : Color.TRANSPARENT);
            // On the view, not the drawable: the network icon swaps its drawable as the signal changes.
            if (has) iv.setColorFilter(new PorterDuffColorFilter(Style.FOCUS_INK, PorterDuff.Mode.SRC_IN));
            else iv.clearColorFilter();
            iv.setAlpha(has ? 1f : ICON_DIM);
            v.animate().scaleX(has ? FOCUS_SCALE : 1f).scaleY(has ? FOCUS_SCALE : 1f).setDuration(has ? 220 : 160)
                    .setInterpolator(has ? new OvershootInterpolator(1.6f) : new DecelerateInterpolator())
                    .withLayer().start();
        });
        iv.setAlpha(ICON_DIM);
        card.setOnClickListener(v -> open(key));
        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(card, new FrameLayout.LayoutParams(b, b));
        // The button is wider than its icon: pull the neighbours in, so the gaps match the left line's.
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(b, b);
        lp.leftMargin = lp.rightMargin = -(b - size) / 2;
        topRight.addView(wrap, 2, lp);
        tiles.add(card);
        return iv;
    }

    private void open(String key) {
        preview.stop(); // free the input before its full-screen player wants it
        Intent i;
        if (ADD.equals(key)) {
            // Add one, or put the apps in order: "arrange" starts moving the first app (arrows move it,
            // OK sets it down); any app can also be moved by holding OK on it.
            dropdown(tile(ADD), ADD_ITEMS, which -> {
                if (which == 0) showAddMenu(-1);
                else startArrange();
            });
            return;
        } else if (LIVE.equals(key)) {
            openLiveTv();
            return;
        } else if (SETTINGS.equals(key)) {
            dropdown(tile(SETTINGS), SETTINGS_ITEMS, which -> {
                if (which == 0) showWallpapers();
                else if (which == 1) showTiles();
                else if (which == 2) showStatusItems();
                else if (which == 3) openBrightness();
                else openTvSettings();
            });
            return;
        } else if (isInput(key)) {
            openInput(key);
            return;
        } else if (NETWORK.equals(key)) {
            openNetworkSettings();
            return;
        } else if (key.startsWith(QUICK)) {
            openQuick(key.substring(QUICK.length()));
            return;
        } else {
            i = launchIntent(getPackageManager(), key);
            if (i == null) return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) {}
    }

    // Live TV apps an HDMI tile can open, in order. Droidlogic (Amlogic) TVs ship Live TV under their
    // own package, with TvActivity as its entry point.
    private static final String[][] LIVE_TV = {{"com.android.tv", "com.android.tv.MainActivity"},
            {"com.droidlogic.android.tv", "com.android.tv.TvActivity"}};
    private static final String INPUT_PLAYER = "com.google.android.tv.inputplayer";

    // Opens an HDMI input: the stock Live TV app first, then Droidlogic's, then whatever the TV picks.
    // Each one is tried only when the one before it can't start, so a TV where the stock app already
    // worked takes exactly the same path as before. The one exception: when the TV itself sends HDMI to
    // Google TV's input player, that goes first, because some Google TVs ship com.android.tv as a stub
    // that starts fine but only says "Hello World!" (issue #5).
    private void openInput(String id) {
        Intent view = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id));
        List<Intent> tries = new ArrayList<>();
        ResolveInfo def = getPackageManager().resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY);
        if (def != null && def.activityInfo != null && INPUT_PLAYER.equals(def.activityInfo.packageName))
            tries.add(new Intent(view).setClassName(INPUT_PLAYER, def.activityInfo.name));
        for (String[] live : LIVE_TV) {
            Intent t = new Intent(view).setClassName(live[0], live[1]);
            if (t.resolveActivity(getPackageManager()) != null) tries.add(t);
        }
        tries.add(view);
        for (Intent t : tries) {
            t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(t);
                return;
            } catch (Exception ignored) {}
        }
    }

    // The shortcuts picked with --es quick, in that order, as indexes into QUICK_NAMES. A shortcut
    // this TV has no screen for is left out, so it never shows a tile that does nothing.
    private List<Integer> quickList() {
        List<Integer> out = new ArrayList<>();
        String saved = getSharedPreferences(PREFS, 0).getString("quick", null);
        if (saved == null) return out;
        for (String name : saved.split("[,\\s]+")) {
            int q = Arrays.asList(QUICK_NAMES).indexOf(name.toLowerCase(java.util.Locale.ROOT));
            if (q < 0 || out.contains(q) || quickIntents(QUICK_NAMES[q]).isEmpty()) continue;
            out.add(q);
        }
        return out;
    }

    // OFFLINE, 1 to 3 for Wi-Fi with that many bars, or ETHERNET (any other link, which on a TV is a cable).
    private int networkState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return OFFLINE;
        try {
            int rssi = Integer.MIN_VALUE;
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities c = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (c == null) return OFFLINE;
                if (!c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return ETHERNET;
                if (android.os.Build.VERSION.SDK_INT >= 29) rssi = c.getSignalStrength();
            } else {
                android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni == null || !ni.isConnected()) return OFFLINE;
                if (ni.getType() != ConnectivityManager.TYPE_WIFI) return ETHERNET;
            }
            if (rssi == Integer.MIN_VALUE) { // Android 9 and older: ask the Wi-Fi service
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null && wm.getConnectionInfo() != null) rssi = wm.getConnectionInfo().getRssi();
            }
            return 1 + WifiManager.calculateSignalLevel(rssi, 3);
        } catch (Exception e) {
            return OFFLINE;
        }
    }

    private void refreshNetwork() {
        if (netIcon == null) return;
        int state = networkState();
        if (state == netState) return;
        netState = state;
        // Always beside the clock, and it follows the connection by itself: Wi-Fi with its bars, a cable
        // for Ethernet (LAN), crossed out when offline.
        if (state == ETHERNET) {
            netIcon.setImageDrawable(new LineIcon(LineIcon.ETHERNET));
        } else {
            // The whole Wi-Fi fan dim, with the bars it has bright on top. Offline is a slash instead.
            LineIcon fan = new LineIcon(LineIcon.WIFI);
            fan.setAlpha(0x47);
            int[] top = state == OFFLINE ? LineIcon.SLASH : state == 1 ? LineIcon.WIFI_1
                    : state == 2 ? LineIcon.WIFI_2 : LineIcon.WIFI;
            netIcon.setImageDrawable(new LayerDrawable(new Drawable[] {fan, new LineIcon(top)}));
        }
        ((View) netIcon.getParent()).setContentDescription(
                state == OFFLINE ? Strings.t("Offline") : state == ETHERNET ? "Ethernet" : "Wi-Fi");
    }

    // The status bar's choices, on or off, each saved at once. Clock and network change at the top right,
    // the rest in the line at the top left.
    private void showStatusItems() {
        final boolean[] on = new boolean[STATUS_ITEMS.length];
        for (int i = 0; i < on.length; i++) on[i] = statusOn(STATUS_ITEMS[i]);
        panel(Strings.t("Status bar"), Arrays.asList(translated(STATUS_LABELS)), null, 0, -1, null, on, which -> {
            List<String> keep = new ArrayList<>();
            for (int i = 0; i < on.length; i++) if (on[i]) keep.add(STATUS_ITEMS[i]);
            getSharedPreferences(PREFS, 0).edit().putString("status", android.text.TextUtils.join(",", keep)).apply();
            placeTopRight();
            restartStatus();
        });
    }

    private boolean statusOn(String item) {
        String saved = getSharedPreferences(PREFS, 0).getString("status", STATUS_DEFAULT);
        return Arrays.asList(saved.split(",")).contains(item);
    }

    // The top right row: what's switched on, with a dot between each two.
    private void placeTopRight() {
        if (netIcon == null) return;
        boolean rate = statusOn("Download speed"), net = statusOn("Network"), clock = statusOn("Clock");
        rateText.setVisibility(rate ? View.VISIBLE : View.GONE);
        ((View) netIcon.getParent().getParent()).setVisibility(net ? View.VISIBLE : View.GONE); // gone: out of the focus order too
        clockView.setVisibility(clock ? View.VISIBLE : View.GONE);
        rateDot.setVisibility(rate && (net || clock) ? View.VISIBLE : View.GONE);
        clockDot.setVisibility(net && clock ? View.VISIBLE : View.GONE);
    }

    private TextView statusText() {
        TextView t = text(px(Style.LABEL), Style.INK_2, semibold);
        t.setFontFeatureSettings("tnum");
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setSingleLine(true);
        return t;
    }

    // The status line, every two seconds while the home screen is in front: the CPU's speed, RAM as the
    // share in use and/or in GB, the download speed for the whole TV. CPU use in percent can't be had:
    // Android keeps /proc/stat, /proc/uptime, /proc/loadavg and the cgroup's cpu.stat from apps, the
    // cpuinfo service only has a minutes-old average, and this chip keeps no idle statistics (all tested).
    // Its "ondemand" governor raises the speed with the load, so the speed is the next best thing.
    private TextView statusLine, rateText, rateDot, clockDot;
    private LinearLayout topRight;
    private long rxBytes = -1, rxAt;
    private final Runnable statusTick = new Runnable() {
        @Override public void run() {
            updateStatus();
            followDayAndNight();
            root.postDelayed(this, 2000);
        }
    };

    private void restartStatus() {
        root.removeCallbacks(statusTick);
        rxBytes = -1;
        statusTick.run();
    }

    private void updateStatus() {
        List<String> parts = new ArrayList<>();
        java.util.Locale loc = java.util.Locale.getDefault();
        if (statusOn("Date"))
            parts.add(android.text.format.DateFormat.format(Strings.datePattern(), System.currentTimeMillis()).toString());
        if (statusOn("CPU")) {
            String clock = cpuClock(loc);
            if (clock != null) parts.add(clock);
        }
        if (statusOn("RAM") || statusOn("RAM in GB")) {
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long used = mi.totalMem - mi.availMem;
            if (mi.totalMem > 0 && statusOn("RAM"))
                parts.add(String.format(loc, "RAM %d %%", Math.round(100.0 * used / mi.totalMem)));
            if (mi.totalMem > 0 && statusOn("RAM in GB"))
                parts.add(String.format(loc, "RAM %.1f / %.1f GB", used / 1e9, mi.totalMem / 1e9));
        }
        String rate = "\u2193 0 KB/s";
        if (statusOn("Download speed")) {
            long rx = android.net.TrafficStats.getTotalRxBytes(), now = android.os.SystemClock.elapsedRealtime();
            if (rx >= 0 && rxBytes >= 0 && now > rxAt) {
                double perSec = (rx - rxBytes) * 1000.0 / (now - rxAt);
                rate = perSec >= 1e6 ? String.format(loc, "\u2193 %.1f MB/s", perSec / 1e6)
                        : String.format(loc, "\u2193 %d KB/s", Math.round(perSec / 1e3));
            }
            rxBytes = rx;
            rxAt = now;
        }
        if (!rate.contentEquals(rateText.getText())) rateText.setText(rate);
        if (statusOn("Off timer")) {
            long left = getSharedPreferences(PREFS, 0).getLong("offAt", 0) - System.currentTimeMillis();
            if (left > 0) parts.add(Strings.t("TV off in") + " " + (left + 59_999) / 60_000 + " " + Strings.t("min"));
        }
        String line = android.text.TextUtils.join(STATUS_DOT, parts);
        if (!line.contentEquals(statusLine.getText())) statusLine.setText(line);
    }

    // The cores' current speed, averaged.
    private static String cpuClock(java.util.Locale loc) {
        long sum = 0;
        int n = 0;
        for (int i = 0; i < 16; i++) {
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(
                    "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq"))) {
                sum += Long.parseLong(r.readLine().trim());
                n++;
            } catch (Exception e) {
                if (i > 0) break;
            }
        }
        return n == 0 ? null : String.format(loc, "CPU %.1f GHz", sum / (double) n / 1e6);
    }

    // Wi-Fi settings (on Android TV that page lists Ethernet too), else the TV's network page, else Settings.
    private void openNetworkSettings() {
        for (String a : new String[] {Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS,
                Settings.ACTION_SETTINGS}) {
            try {
                startActivity(new Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {}
        }
    }

    // Screens a shortcut can open, best first, keeping only those this TV has.
    private List<Intent> quickIntents(String name) {
        List<Intent> all = new ArrayList<>();
        String tclType = name.equals("sound") ? "SoundPreset" : name.equals("picture") ? "picture" : null;
        if (tclType != null)
            all.add(new Intent(TCL_SHOW_WINDOW).setClassName("com.tcl.settings", "com.tcl.settings.ShowWindowService")
                    .putExtra("Type", tclType));
        switch (name) {
            case "bluetooth":
                all.add(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                all.add(new Intent("com.google.android.intent.action.CONNECT_INPUT")); // Android TV's pairing screen
                break;
            case "sound": all.add(new Intent(Settings.ACTION_SOUND_SETTINGS)); break;
            case "picture": all.add(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); break;
        }
        PackageManager pm = getPackageManager();
        List<Intent> out = new ArrayList<>();
        for (Intent i : all) {
            boolean service = TCL_SHOW_WINDOW.equals(i.getAction());
            if (service ? pm.resolveService(i, 0) != null : i.resolveActivity(pm) != null) out.add(i);
        }
        return out;
    }

    // Opens a shortcut's screen: the first one that starts wins. False if none did.
    private boolean openQuick(String name) {
        // Android 12+ only lets apps with "Nearby devices" open the Bluetooth pairing screen (Android TV
        // has no other Bluetooth page). Ask once; the answer comes back in onRequestPermissionsResult.
        if (name.equals("bluetooth") && android.os.Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(BT_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {BT_CONNECT}, 1);
            return true;
        }
        for (Intent i : quickIntents(name)) {
            try {
                if (TCL_SHOW_WINDOW.equals(i.getAction())) startService(i);
                else startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static final String BT_CONNECT = "android.permission.BLUETOOTH_CONNECT";

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != 1) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) openQuick("bluetooth");
        else openTvSettings(); // without it, the TV's settings still reach Bluetooth
    }

    // The TV's own picture screen, where brightness lives: the Picture shortcut's screens first (TCL's
    // panel, then Android's Display page), then Android TV's Display & Sound page, else TV settings.
    // Only system screens are started, so it needs no permission and changes nothing by itself.
    private void openBrightness() {
        if (openQuick("picture")) return;
        Intent i = new Intent().setClassName(TV_DISPLAY_SOUND[0], TV_DISPLAY_SOUND[1]);
        if (i.resolveActivity(getPackageManager()) != null) {
            try {
                startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {}
        }
        openTvSettings();
    }

    private void openTvSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception ignored) {}
    }

    interface Pick { void at(int which); }
    interface Icon { Drawable at(int which) throws Exception; }

    private void panel(String title, List<String> labels, Pick pick) {
        panel(title, labels, null, 0, -1, null, pick);
    }

    // A side panel in the same look as Control Center: it floats at the right, a title, then one row
    // per choice; the selected row is white with dark text, like every focus here. Back closes it.
    // icon (or null) puts an icon of that size by each name. With preview, the list starts on selected
    // and moving through it calls preview; Back calls it with selected again, to put things back.
    private void panel(String title, final List<String> labels, final Icon icon, final int size,
                       final int selected, final Pick preview, final Pick pick) {
        panel(title, labels, icon, size, selected, preview, null, pick);
    }

    // With checks, each row is a switch: a tick at the end while on. OK flips it, calls pick and keeps
    // the panel open; Back closes it.
    private void panel(String title, final List<String> labels, final Icon icon, final int size,
                       final int selected, final Pick preview, final boolean[] checks, final Pick pick) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(Style.PANEL_PAD), px(Style.PANEL_PAD + 12), px(Style.PANEL_PAD), px(Style.PANEL_PAD));
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setCornerRadius(px(Style.RADIUS_PANEL));
        panelBg.setColor(Style.PANEL);
        panelBg.setStroke(Math.max(1, px(Style.STROKE)), Style.GLASS_STROKE);
        box.setBackground(panelBg);

        TextView head = text(px(Style.TITLE), Style.INK, semibold);
        head.setText(title);
        head.setSingleLine(true);
        head.setEllipsize(android.text.TextUtils.TruncateAt.END);
        head.setPadding(px(20), 0, px(20), 0);
        box.addView(head);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setVerticalFadingEdgeEnabled(true); // rows fade out under the title, not cut off
        list.setFadingEdgeLength(px(48));
        GradientDrawable focus = new GradientDrawable();
        focus.setCornerRadius(px(Style.RADIUS_TILE));
        focus.setColor(Style.FOCUS);
        list.setSelector(focus);
        // The selected row's text turns dark on the white selector (ListView marks that row selected).
        final android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                new int[][] {{android.R.attr.state_selected}, {}}, new int[] {Style.FOCUS_INK, Style.INK});
        // Icons load as rows appear, drawn small to save RAM.
        final Drawable[] icons = new Drawable[labels.size()];
        list.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return labels.size(); }
            @Override public Object getItem(int pos) { return labels.get(pos); }
            @Override public long getItemId(int pos) { return pos; }
            @Override public View getView(int pos, View convert, ViewGroup parent) {
                TextView t = convert != null ? (TextView) convert : text(px(Style.BODY), Style.INK, regular);
                t.setTextColor(ink);
                t.setText(labels.get(pos));
                t.setSingleLine(true);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setMinHeight(px(Style.ROW_H));
                t.setPadding(px(20), 0, px(20), 0);
                if (icon != null && icons[pos] == null) {
                    try { icons[pos] = icon.at(pos); }
                    catch (Exception e) { icons[pos] = new ColorDrawable(Color.TRANSPARENT); }
                    icons[pos].setBounds(0, 0, size, size);
                }
                Drawable tick = null;
                if (checks != null) {
                    tick = checks[pos] ? new LineIcon(LineIcon.CHECK) : new ColorDrawable(Color.TRANSPARENT);
                    tick.setBounds(0, 0, px(36), px(36));
                    if (checks[pos]) tick.setTintList(ink); // dark on the white selector, like the text; not the swatches
                }
                t.setCompoundDrawablesRelative(icons[pos], null, tick, null);
                t.setCompoundDrawablePadding(px(24));
                return t;
            }
        });
        list.setOnItemClickListener((parent, v, pos, id) -> {
            if (checks != null) {
                checks[pos] = !checks[pos];
                pick.at(pos); // may set other rows too
                ((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
                return;
            }
            d.dismiss();
            pick.at(pos);
        });
        if (preview != null) {
            list.setSelection(selected);
            list.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                    preview.at(pos);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
            d.setOnCancelListener(x -> preview.at(selected));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = px(24);
        box.addView(list, lp);

        // Floating, as far from the screen's edges as the dock.
        FrameLayout frame = new FrameLayout(this);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -1);
        bp.setMargins(0, px(Style.PANEL_MARGIN), px(Style.PANEL_MARGIN), px(Style.PANEL_MARGIN));
        frame.addView(box, bp);
        d.setContentView(frame);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(px(Style.PANEL_W + Style.PANEL_MARGIN), -1);
        w.setGravity(Gravity.END);
        // A preview, or switches that change the page at once, need the home screen undimmed.
        w.setDimAmount(preview != null || checks != null ? 0f : 0.6f);
        d.show();
    }

    // A small menu that drops down under a tile, in the panels' look: a dark panel, the focused row
    // white with dark text. Opens above the tile when there's no room below. Back closes it.
    private void dropdown(View anchor, String[] items, final Pick pick) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        // The panel's corner minus its padding is the rows' corner: 40 - 16 = 24, so the curves nest.
        int pad = px(Style.RADIUS_PANEL - Style.RADIUS_TILE), rowH = px(72);
        box.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(Style.RADIUS_PANEL));
        bg.setColor(Style.PANEL);
        bg.setStroke(Math.max(1, px(Style.STROKE)), Style.GLASS_STROKE);
        box.setBackground(bg);
        float widest = 0; // some languages need more room than the tile is wide
        for (int i = 0; i < items.length; i++) {
            final int which = i;
            final TextView t = text(px(Style.LABEL), Style.INK, regular);
            t.setText(Strings.t(items[i]));
            t.setSingleLine(true);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(px(22), 0, px(22), 0);
            widest = Math.max(widest, t.getPaint().measureText(t.getText().toString()) + 2 * px(22) + 2 * pad);
            final GradientDrawable focus = new GradientDrawable();
            focus.setCornerRadius(px(Style.RADIUS_TILE));
            focus.setColor(Color.TRANSPARENT);
            t.setBackground(focus);
            t.setFocusable(true);
            t.setOnFocusChangeListener((v, has) -> {
                focus.setColor(has ? Style.FOCUS : Color.TRANSPARENT);
                t.setTextColor(has ? Style.FOCUS_INK : Style.INK);
            });
            t.setOnClickListener(v -> {
                d.dismiss();
                pick.at(which);
            });
            box.addView(t, new LinearLayout.LayoutParams(-1, rowH));
        }
        d.setContentView(box);

        // Line it up with the tile as drawn (focused tiles are scaled up), below its name.
        int[] at = new int[2];
        anchor.getLocationOnScreen(at);
        int tileBottom = at[1] + Math.round(anchor.getHeight() * anchor.getScaleY());
        int w = Math.max(Math.max(Math.round(anchor.getWidth() * anchor.getScaleX()), px(320)), (int) Math.ceil(widest));
        int h = items.length * rowH + 2 * pad;
        int y = tileBottom + px(64);
        if (y + h > getResources().getDisplayMetrics().heightPixels - px(24)) y = at[1] - h - px(16);
        Window win = d.getWindow();
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        win.setGravity(Gravity.TOP | Gravity.START);
        android.view.WindowManager.LayoutParams lp = win.getAttributes();
        lp.x = at[0];
        lp.y = y;
        lp.width = w;
        lp.height = h;
        win.setAttributes(lp);
        d.show();
    }

    // Hold OK (or Menu) on a top-row app: move it, swap it for another, or take it off the home
    // screen. Removing only takes the tile away: the app stays installed and can be added back.
    // Apps the user installed can also be uninstalled; Android asks to confirm, and the tile goes
    // once the app is gone. Apps that came with the TV can't be, so they get App info instead,
    // where Android offers to turn them off.
    private void showEditMenu(final String key) {
        final int index = rowApps.indexOf(key);
        if (index < 0) return;
        final boolean user = userInstalled(key);
        dropdown(tile(key), user ? EDIT_ITEMS : EDIT_ITEMS_SYSTEM, which -> {
            if (which == 0) startMove(key);
            else if (which == 1) showAddMenu(index);
            else if (which == 3) {
                Uri pkg = Uri.parse("package:" + key);
                try { startActivity(user ? new Intent(Intent.ACTION_DELETE, pkg)
                        : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)); }
                catch (Exception ignored) {}
            } else {
                rowApps.remove(index);
                saveRow();
                focusedKey = rowApps.isEmpty() ? ADD : rowApps.get(Math.min(index, rowApps.size() - 1));
                buildTiles();
            }
        });
    }

    // Every launchable app that isn't on the top row yet, by name. The pick is added at the end,
    // or takes the place of the app at replaceIndex (-1 to add).
    private void showAddMenu(final int replaceIndex) {
        final PackageManager pm = getPackageManager();
        TreeMap<String, String> byLabel = launchableApps(true);
        if (byLabel.isEmpty()) {
            panel(Strings.t("Every app is already here"), Collections.singletonList("OK"), which -> {});
            return;
        }
        final List<String> pkgs = new ArrayList<>(byLabel.values());
        String title = replaceIndex < 0 ? Strings.t("Add an app")
                : String.format(Strings.t("Replace %s"), labelOf(rowApps.get(replaceIndex)));
        panel(title, new ArrayList<>(byLabel.keySet()), pos -> fit(pm.getApplicationIcon(pkgs.get(pos)), px(48), px(48)),
                px(48), -1, null, which -> {
            if (replaceIndex < 0) rowApps.add(pkgs.get(which));
            else rowApps.set(replaceIndex, pkgs.get(which));
            saveRow();
            focusedKey = pkgs.get(which);
            buildTiles();
        });
    }

    // Every app that can be opened, by name. skipRow leaves out the apps already on the home screen.
    private TreeMap<String, String> launchableApps(boolean skipRow) {
        PackageManager pm = getPackageManager();
        TreeMap<String, String> byLabel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if ((skipRow && rowApps.contains(p)) || byLabel.containsValue(p) || p.equals(getPackageName())
                        || (NOT_APPS.contains(p) && !p.equals(PLAY_STORE))) continue;
                String label = labelOf(p);
                if (!byLabel.containsKey(label)) byLabel.put(label, p);
            }
        }
        return byLabel;
    }

    // The remote's Apps button: every app, A to Z, OK opens it.
    private void showAllApps() {
        final PackageManager pm = getPackageManager();
        TreeMap<String, String> byLabel = launchableApps(false);
        final List<String> pkgs = new ArrayList<>(byLabel.values());
        panel(Strings.t("All apps"), new ArrayList<>(byLabel.keySet()),
                pos -> fit(pm.getApplicationIcon(pkgs.get(pos)), px(48), px(48)), px(48), -1, null,
                which -> open(pkgs.get(which)));
    }

    // Live TV for the channel, number and guide buttons: the stock app, Droidlogic's, MediaTek's (most
    // Google TVs of this kind, Peaq too), then TCL's. The first one this TV has opens.
    private static final String[] LIVE_TV_APPS = {"com.android.tv", "com.droidlogic.android.tv",
            "com.mediatek.wwtv.tvcenter", "com.tcl.tv"};

    private Intent liveTvIntent() {
        return liveTvIntent(this);
    }

    static Intent liveTvIntent(Context c) {
        PackageManager pm = c.getPackageManager();
        for (String p : LIVE_TV_APPS) {
            Intent i = launchIntent(pm, p);
            if (i != null) return i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return null;
    }

    private boolean openLiveTv() {
        Intent i = liveTvIntent();
        if (i == null) return false;
        try {
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Remote buttons that reach the home screen because no app is in front. The rest (Home, volume,
    // power, the app buttons) Android handles before they get here.
    @Override
    public boolean onKeyDown(int code, KeyEvent ev) {
        if (movingKey != null) return super.onKeyDown(code, ev);
        if (code == KeyEvent.KEYCODE_ALL_APPS) {
            if (ev.getRepeatCount() == 0) showAllApps();
            return true;
        }
        boolean live = (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9)
                || code == KeyEvent.KEYCODE_CHANNEL_UP || code == KeyEvent.KEYCODE_CHANNEL_DOWN
                || code == KeyEvent.KEYCODE_GUIDE || code == KeyEvent.KEYCODE_TV
                || code == KeyEvent.KEYCODE_LAST_CHANNEL;
        if (live) {
            if (ev.getRepeatCount() == 0) openLiveTv();
            return true;
        }
        return super.onKeyDown(code, ev);
    }

    private static String[] translated(String[] en) {
        String[] out = new String[en.length];
        for (int i = 0; i < en.length; i++) out[i] = Strings.t(en[i]);
        return out;
    }

    private String labelOf(String pkg) {
        PackageManager pm = getPackageManager();
        try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    // Saves the top row as the app list, so it no longer follows apps.txt.
    private void saveRow() {
        getSharedPreferences(PREFS, 0).edit().putString("apps", android.text.TextUtils.join(",", rowApps)).apply();
    }

    // Where app number i sits in the grid.
    // Scrolls the app row just enough to show app number i, in whole steps, so the tiles always sit on
    // the dock's grid: the first one where it starts, the last one where the sixth would end.
    private void scrollRowTo(int i) {
        if (i < 0) return;
        int visibleRight = appScroll.getWidth() - (px(TILE_LEFT) - stripX);
        int x = appScroll.getScrollX();
        if (gridX(i) - x < gridX(0)) x = gridX(i) - gridX(0);
        if (gridX(i) + tileW - x > visibleRight) x = gridX(i) + tileW - visibleRight;
        x = Math.max(0, x);
        appScroll.smoothScrollTo(x, 0);
        showArrows(x);
    }

    // ‹ and › beside the app row while there are apps out of sight that way. scrollX is where the
    // row is going, as it may still be sliding there.
    private void showArrows(int scrollX) {
        int max = Math.max(0, appStrip.getWidth() - appScroll.getWidth());
        moreLeft.animate().alpha(scrollX > 0 ? 1f : 0f).setDuration(200).start();
        moreRight.animate().alpha(scrollX < max ? 1f : 0f).setDuration(200).start();
    }

    // Where app number i sits in the app row (inside appStrip).
    private int gridX(int i) {
        return px(TILE_LEFT) - stripX + i * (tileW + px(GAP));
    }

    private int gridY(int i) {
        return gridTop - stripY;
    }

    private View tile(String key) {
        for (View t : tiles) if (t.getTag().equals(key)) return t;
        return null;
    }

    // Move mode: the tile gets an outline, the arrows move it through the grid, OK or Back finishes.
    private void startMove(String key) {
        View card = tile(key);
        if (card == null) return;
        movingKey = key;
        GradientDrawable outline = new GradientDrawable();
        outline.setCornerRadius(px(RADIUS));
        outline.setStroke(px(4), Color.WHITE);
        ((FrameLayout) card).setForeground(outline);
        showHint();
        card.requestFocus();
    }

    // Arranging ("Arrange apps" under Add app): pick up any app with OK, move it, put it down with OK,
    // then the next; Back (or leaving the app row) ends it. Holding OK on an app moves just that one.
    private boolean arranging;

    private void startArrange() {
        if (rowApps.isEmpty()) return;
        arranging = true;
        showHint();
        View first = tile(rowApps.get(0));
        if (first != null) first.requestFocus();
    }

    private void stopArrange() {
        arranging = false;
        showHint();
    }

    // The line above the dock that says what the arrows and OK do right now, if anything special.
    private void showHint() {
        if (movingKey != null) moveHint.setText(Strings.t("Use the arrows to move it, then press OK"));
        else if (arranging) moveHint.setText(Strings.t("Pick an app with OK, move it, OK puts it down. Back: done"));
        moveHint.setVisibility(movingKey != null || arranging ? View.VISIBLE : View.GONE);
    }

    private boolean moveKey(String key, int code, boolean ok, boolean down) {
        int step = code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 0;
        if (step != 0) {
            int from = rowApps.indexOf(key), to = from + step;
            if (down && to >= 0 && to < rowApps.size()) {
                // Slide the tiles into their new places: no rebuild, so it stays quick on slow TVs.
                rowApps.add(to, rowApps.remove(from));
                for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
                    View wrap = (View) tile(rowApps.get(i)).getParent();
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) wrap.getLayoutParams();
                    lp.leftMargin = gridX(i);
                    lp.topMargin = gridY(i);
                    wrap.setLayoutParams(lp);
                }
                scrollRowTo(to); // scroll along with it
            }
            return true;
        }
        if (ok || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_MENU) {
            if (!down) {
                movingKey = null;
                longPressed = false;
                ((FrameLayout) tile(key)).setForeground(null);
                showHint(); // back to "pick an app" while arranging
                saveRow();
                refreshApps();
            }
            return true;
        }
        return true; // up/down and everything else stay put while moving
    }

    private static boolean isInput(String key) {
        return key.indexOf('/') > 0;
    }

    // The TV's own HDMI inputs (not devices behind them), in order, skipping hidden ones.
    // HDMI-CEC devices that are on, by the HDMI input they're plugged into, with their names. Android adds
    // an input of their own for them (parent: the HDMI port). Needs HDMI-CEC on in the TV's settings;
    // without it, or for a device that's fully off, the port just has no dot. (The plain HDMI inputs
    // can't tell: on MediaTek they say "connected" whether anything is plugged in or not.)
    private void refreshCecDevices() {
        cecDevices.clear();
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return;
        for (TvInputInfo in : tim.getTvInputList()) {
            if (in.getParentId() == null || in.getType() != TvInputInfo.TYPE_HDMI) continue;
            if (tim.getInputState(in.getId()) == TvInputManager.INPUT_STATE_DISCONNECTED) continue;
            CharSequence name = in.loadLabel(this);
            // CEC names have no spaces when they don't fit ("NintendoSwitch"): put them back.
            cecDevices.put(in.getParentId(), name == null ? "" : name.toString().replaceAll("(?<=[a-z])(?=[A-Z])", " "));
        }
    }

    private List<TvInputInfo> hdmiInputs() {
        List<TvInputInfo> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        for (TvInputInfo in : tim.getTvInputList()) {
            if (in.getType() == TvInputInfo.TYPE_HDMI && in.getParentId() == null && !in.isHidden(this)) out.add(in);
        }
        Collections.sort(out, (a, b) -> a.getId().compareTo(b.getId()));
        return out.size() > MAX_INPUTS ? out.subList(0, MAX_INPUTS) : out;
    }

    // The name set in Settings > Inputs (like "Bedroom"), else the TV's own name (like "HDMI 1").
    private String inputLabel(TvInputInfo in) {
        CharSequence custom = in.loadCustomLabel(this);
        if (custom != null && custom.length() > 0) return withoutDeviceType(custom.toString());
        CharSequence label = in.loadLabel(this);
        return label != null && label.length() > 0 ? withoutDeviceType(label.toString()) : "HDMI";
    }

    // Philips stores a name as "name&DEVICE_TYPE" (like "Computer&COMPUTER"), so show the name only.
    // Only on Philips TVs: elsewhere a name like "AUDIO&VIDEO" is the name itself.
    private static String withoutDeviceType(String label) {
        if (!"Philips".equalsIgnoreCase(android.os.Build.MANUFACTURER)) return label;
        int amp = label.lastIndexOf('&');
        return amp > 0 && label.substring(amp + 1).matches("[A-Z][A-Z0-9_]{2,}") ? label.substring(0, amp).trim() : label;
    }

    private boolean userInstalled(String pkg) {
        try { return (getPackageManager().getApplicationInfo(pkg, 0).flags & ApplicationInfo.FLAG_SYSTEM) == 0; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i != null ? i : pm.getLaunchIntentForPackage(pkg);
    }

    // Xiaomi TVs ship MiLan Pro but don't register it by name; every other TV gets Roboto.
    private static void loadFonts() {
        if (regular != null) return;
        regular = fromFile("/system/fonts/MILanPro_Normal.ttf", Typeface.create("sans-serif", Typeface.NORMAL));
        semibold = fromFile("/system/fonts/MILanPro_SemiBold.ttf", Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    private static Typeface fromFile(String path, Typeface fallback) {
        try { return new File(path).exists() ? Typeface.createFromFile(path) : fallback; }
        catch (Exception e) { return fallback; }
    }

    // Draws a banner once into a tile-sized bitmap (center-cropped). Some apps ship huge banners
    // that Android 9 refuses to draw ("Canvas: trying to draw too large bitmap"); this also saves RAM.
    private Drawable fit(Drawable d, int w, int h) {
        return fit(getResources(), d, w, h);
    }

    static Drawable fit(android.content.res.Resources res, Drawable d, int w, int h) {
        try {
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
            if (iw <= 0 || ih <= 0) { iw = w; ih = h; }
            float s = Math.max((float) w / iw, (float) h / ih);
            int dw = Math.round(iw * s), dh = Math.round(ih * s);
            d.setBounds((w - dw) / 2, (h - dh) / 2, (w - dw) / 2 + dw, (h - dh) / 2 + dh);
            d.draw(new Canvas(b));
            return new BitmapDrawable(res, b);
        } catch (Throwable t) {
            return d;
        }
    }

    // Each tile's wash and sharp art, made once: making and uploading them on every focus change made
    // moving along the tiles stutter. New drawables over the same bitmaps, so two fades can share one.
    private final java.util.Map<String, Drawable[]> shelfCache = new java.util.HashMap<>();

    private Drawable[] shelfFor(String key, Drawable art) {
        Drawable[] made = shelfCache.get(key);
        if (made == null) {
            made = new Drawable[] {shelfWash(fit(art, 160, 52)), fit(art, px(ART_W), px(ART_H))};
            shelfCache.put(key, made);
        }
        return new Drawable[] {fresh(made[0]), fresh(made[1])};
    }

    private static Drawable fresh(Drawable d) {
        Drawable.ConstantState s = d.getConstantState();
        return s == null ? d : s.newDrawable();
    }

    private void crossFade(ImageView v, Drawable next) {
        if (next == null) next = new ColorDrawable(Color.TRANSPARENT);
        Drawable prev = v.getDrawable();
        if (prev instanceof TransitionDrawable) prev = ((TransitionDrawable) prev).getDrawable(1);
        if (prev == null) prev = new ColorDrawable(Color.TRANSPARENT);
        TransitionDrawable fade = new TransitionDrawable(new Drawable[] {prev, next});
        fade.setCrossFadeEnabled(true);
        v.setImageDrawable(fade);
        fade.startTransition(320);
    }

    // The shelf's wash, made in a small bitmap: blurred with two box blurs, darker on the left so the
    // name reads, and faded to nothing at the bottom, so it melts into whatever wallpaper is behind.
    // At 160x52 pixels this takes well under a millisecond.
    private Drawable shelfWash(Drawable d) {
        return wash(getResources(), d, true);
    }

    // The same wash, for the screensaver too; fadeOut false keeps it whole down to the bottom.
    static Drawable wash(android.content.res.Resources res, Drawable d, boolean fadeOut) {
        if (!(d instanceof BitmapDrawable)) return d;
        Bitmap b = ((BitmapDrawable) d).getBitmap();
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h], tmp = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 2; pass++) {
            boxBlur(px, tmp, w, h, 3, true);
            boxBlur(tmp, px, w, h, 3, false);
        }
        for (int y = 0; y < h; y++) {
            float fy = (float) y / (h - 1);
            float alpha = !fadeOut ? 1f : 0.85f * (fy < 0.45f ? 1f : smooth(1f - (fy - 0.45f) / 0.55f));
            // On the shelf, a shade at the top for the clock (it used to be a layer of its own).
            float top = !fadeOut ? 1f : 0.65f + 0.35f * smooth(Math.min(1f, fy / 0.33f));
            for (int x = 0; x < w; x++) {
                // At most 45% brightness, a bit more colour: a white banner (YouTube, Netflix) would
                // otherwise turn the shelf light grey behind the white name and clock.
                float light = top * 0.45f * (0.7f + 0.3f * smooth(Math.min(1f, (float) x / (w * 0.6f))));
                int c = px[y * w + x];
                float r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, bl = c & 0xFF;
                float lum = 0.299f * r + 0.587f * g + 0.114f * bl;
                r = lum + (r - lum) * 1.25f; g = lum + (g - lum) * 1.25f; bl = lum + (bl - lum) * 1.25f;
                int a = Math.round((c >>> 24) * alpha);
                px[y * w + x] = a << 24 | clamp(r * light) << 16 | clamp(g * light) << 8 | clamp(bl * light);
            }
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
        BitmapDrawable out = new BitmapDrawable(res, b);
        out.setFilterBitmap(true);
        return out;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    // One box blur along rows (or columns), edges clamped, channel by channel.
    private static void boxBlur(int[] in, int[] out, int w, int h, int r, boolean rows) {
        int lines = rows ? h : w, len = rows ? w : h, n = 2 * r + 1;
        for (int l = 0; l < lines; l++) {
            int a = 0, rd = 0, g = 0, bl = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[idx(l, Math.max(0, Math.min(len - 1, i)), w, rows)];
                a += c >>> 24; rd += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF; bl += c & 0xFF;
            }
            for (int i = 0; i < len; i++) {
                out[idx(l, i, w, rows)] = (a / n) << 24 | (rd / n) << 16 | (g / n) << 8 | (bl / n);
                int c0 = in[idx(l, Math.max(0, i - r), w, rows)];
                int c1 = in[idx(l, Math.min(len - 1, i + r + 1), w, rows)];
                a += (c1 >>> 24) - (c0 >>> 24);
                rd += ((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF);
                g += ((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF);
                bl += (c1 & 0xFF) - (c0 & 0xFF);
            }
        }
    }

    private static int idx(int line, int i, int w, boolean rows) {
        return rows ? line * w + i : i * w + line;
    }

    private int px(int v) {
        return Math.round(v * scale);
    }

    private TextClock clock(String f12, String f24, int px, int color, Typeface tf) {
        TextClock c = new TextClock(this);
        c.setFormat12Hour(f12);
        c.setFormat24Hour(f24);
        c.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        c.setTextColor(color);
        c.setTypeface(tf);
        return c;
    }

    private TextView text(int px, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        if (tf != null) t.setTypeface(tf);
        return t;
    }

    private static FrameLayout.LayoutParams at(int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin = y;
        return lp;
    }
}
