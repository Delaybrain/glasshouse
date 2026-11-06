package com.ansub.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

// Settings > Screensaver > Show now: the screensaver at once, without waiting for the TV to be idle
// (an app can't start Android's screensaver itself). Any button ends it, as with the real one.
public class SaverPreview extends Activity {
    private SaverView view;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        view = new SaverView(this);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(view);
        view.start();
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        view.stop();
        super.onDestroy();
    }
}
