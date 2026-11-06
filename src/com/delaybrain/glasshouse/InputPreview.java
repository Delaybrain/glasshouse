package com.delaybrain.glasshouse;

import android.content.Context;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvView;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

// A live picture of an HDMI input behind the whole home screen while its pill is focused: Android's own
// TvView, tuned to the input's pass-through channel, full screen under the page (the home screen fades
// its wallpaper out and dims the picture, see MainActivity.onPicture). Full screen because that's how
// this MediaTek TV shows video: on its own plane under everything, and a small window left the rest of
// the screen black. It starts a moment after the pill is focused, so moving along the pills doesn't
// switch the TV's video path for each one, and stops when focus moves on or the home screen goes away,
// so the input is free again for the full-screen player.
// It also tells whether a picture came in (a device is on and sending), the one way to know that for
// a device without HDMI-CEC, like a PC. Note: switching to an input sends CEC's "set stream path", which
// can wake a CEC device on that port, the same as switching to it with the remote.
// What another app is showing can't be previewed: Android doesn't let one app see another's screen.
final class InputPreview {
    interface Listener {
        void onSignal(String inputId, boolean picture); // whether this input sends a picture
        void onPicture(boolean showing);                // the picture behind the page came or went
    }

    private static final long DELAY_MS = 700, NO_PICTURE_MS = 3000;
    private final TvView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private String pending, tuned;
    private boolean showing;
    private final Runnable noPicture = () -> {
        if (tuned != null) InputPreview.this.listener.onSignal(tuned, false);
    };

    InputPreview(Context c, Listener listener) {
        this.listener = listener;
        view = new TvView(c);
        view.setVisibility(View.INVISIBLE);
        view.setCallback(new TvView.TvInputCallback() {
            @Override public void onVideoAvailable(String inputId) {
                if (!inputId.equals(tuned)) return;
                handler.removeCallbacks(noPicture);
                show(true);
                listener.onSignal(inputId, true);
            }
            @Override public void onVideoUnavailable(String inputId, int reason) {
                if (!inputId.equals(tuned)) return;
                // A picture that was there went away: the device was turned off or unplugged.
                boolean lost = showing && reason != TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING
                        && reason != TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING;
                show(false);
                if (lost) listener.onSignal(inputId, false);
            }
            @Override public void onConnectionFailed(String inputId) {
                android.util.Log.w("InputPreview", "can't connect to " + inputId);
            }
            // The resolution is often only known a moment after the picture: tell the page again.
            @Override public void onTracksChanged(String inputId, java.util.List<TvTrackInfo> tracks) {
                if (inputId.equals(tuned) && showing) listener.onPicture(true);
            }
        });
    }

    // What's coming in, like "1080p · 60 Hz", from the input's video track; null if the TV doesn't say.
    String videoInfo() {
        try {
            java.util.List<TvTrackInfo> tracks = view.getTracks(TvTrackInfo.TYPE_VIDEO);
            if (tracks == null || tracks.isEmpty()) return null;
            String selected = view.getSelectedTrack(TvTrackInfo.TYPE_VIDEO);
            TvTrackInfo video = tracks.get(0);
            for (TvTrackInfo t : tracks) if (t.getId().equals(selected)) video = t;
            int h = video.getVideoHeight();
            if (h <= 0) return null;
            String size = h >= 2000 ? "4K" : h + "p";
            float fps = video.getVideoFrameRate();
            return fps > 0 ? size + "  ·  " + Math.round(fps) + " Hz" : size;
        } catch (Exception e) {
            return null;
        }
    }

    // The input whose picture is showing behind the page now, or null.
    String showingInput() {
        return showing ? tuned : null;
    }

    View view() {
        return view;
    }

    // Shows this input after a moment; null (another tile focused) stops the preview.
    void preview(String inputId) {
        if (inputId != null && inputId.equals(pending)) return;
        // Another input, or none: end the one running first, so the next starts from a clean session
        // instead of the TV re-routing the running one.
        stop();
        pending = inputId;
        if (inputId == null) return;
        handler.postDelayed(() -> {
            if (!inputId.equals(pending)) return;
            tuned = inputId;
            view.setVisibility(View.VISIBLE);
            handler.postDelayed(noPicture, NO_PICTURE_MS);
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
        show(false);
    }

    private void show(boolean picture) {
        if (picture == showing) return;
        showing = picture;
        listener.onPicture(picture);
    }
}
