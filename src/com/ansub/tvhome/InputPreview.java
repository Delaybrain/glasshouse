package com.ansub.tvhome;

import android.content.Context;
import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

// A live picture of an HDMI input in the top shelf, where the art goes for apps: Android's own TvView,
// tuned to the input's pass-through channel. It starts a moment after the pill is focused, so moving
// along the pills doesn't switch the TV's video path for each one, and stops when focus moves on or
// the home screen goes away, so the input is free again for the full-screen player.
// What another app is showing can't be previewed: Android doesn't let one app see another's screen.
final class InputPreview {
    interface Listener { void onPicture(boolean showing); }

    private static final long DELAY_MS = 700;
    private final TvView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private String pending, tuned;

    InputPreview(Context c, Listener listener) {
        this.listener = listener;
        view = new TvView(c);
        view.setVisibility(View.INVISIBLE);
        view.setCallback(new TvView.TvInputCallback() {
            @Override public void onVideoAvailable(String inputId) {
                if (inputId.equals(tuned)) show(true);
            }
            @Override public void onVideoUnavailable(String inputId, int reason) {
                if (inputId.equals(tuned)) show(false);
            }
            @Override public void onConnectionFailed(String inputId) {
                android.util.Log.w("InputPreview", "can't connect to " + inputId);
                if (inputId.equals(tuned)) show(false);
            }
            @Override public void onDisconnected(String inputId) {
                if (inputId.equals(tuned)) show(false);
            }
        });
    }

    View view() {
        return view;
    }

    // Shows this input after a moment; null (another tile focused) stops the preview.
    void preview(String inputId) {
        handler.removeCallbacksAndMessages(null);
        pending = inputId;
        if (inputId == null) {
            stop();
            return;
        }
        handler.postDelayed(() -> {
            if (!inputId.equals(pending)) return;
            tuned = inputId;
            view.setVisibility(View.VISIBLE);
            view.setAlpha(0f); // fades in once there's a picture
            try {
                view.tune(inputId, TvContract.buildChannelUriForPassthroughInput(inputId));
            } catch (Exception e) {
                android.util.Log.w("InputPreview", "can't tune " + inputId + ": " + e);
                stop();
            }
        }, DELAY_MS);
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
        pending = null;
        if (tuned != null) {
            tuned = null;
            try { view.reset(); } catch (Exception ignored) {}
        }
        view.setVisibility(View.INVISIBLE);
        listener.onPicture(false);
    }

    private void show(boolean picture) {
        view.animate().alpha(picture ? 1f : 0f).setDuration(300).start();
        listener.onPicture(picture);
    }
}
