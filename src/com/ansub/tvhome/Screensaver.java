package com.ansub.tvhome;

import android.service.dreams.DreamService;

// The screensaver Android starts when the TV is idle (see the wait in Control Center). What it shows is
// SaverView: the clock over the apps' colours, or photos.
public class Screensaver extends DreamService {
    // The one showing, if any: HomeGuard ends it when it takes a button the screensaver never sees (Home).
    static Screensaver active;
    private SaverView view;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false); // any button wakes the TV and ends the screensaver
        setFullscreen(true);
        view = new SaverView(this);
        setContentView(view);
        view.start();
        active = this;
    }

    @Override
    public void onDetachedFromWindow() {
        if (active == this) active = null;
        if (view != null) view.stop();
        super.onDetachedFromWindow();
    }
}
