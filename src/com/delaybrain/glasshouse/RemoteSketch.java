package com.delaybrain.glasshouse;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

// A line drawing of a TV remote for the Remote buttons help card: the ring of arrows with OK, Back and
// Home under it, two more buttons, volume and channel. One button is lit (white, with a soft ring):
// "a button like this one". Drawn in the shape of a remote whatever the space, centred in it, in the
// panels' ink colours.
final class RemoteSketch extends Drawable {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lit = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    RemoteSketch(float stroke) {
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(stroke);
        line.setColor(Style.INK_2);
        lit.setColor(Style.FOCUS);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(stroke * 2.5f);
        glow.setColor(0x59FFFFFF);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float h = b.height() * 0.96f, w = Math.min(b.width() * 0.9f, h * 0.36f);
        float x = b.exactCenterX() - w / 2, y = b.exactCenterY() - h / 2, cx = x + w / 2;
        r.set(x, y, x + w, y + h);
        c.drawRoundRect(r, w * 0.45f, w * 0.45f, line);                 // the body
        c.drawCircle(cx, y + h * 0.07f, w * 0.06f, line);               // power
        float ring = y + h * 0.27f;
        c.drawCircle(cx, ring, w * 0.36f, line);                        // the arrows
        c.drawCircle(cx, ring, w * 0.15f, line);                        // OK
        float row1 = y + h * 0.47f, row2 = y + h * 0.57f, left = cx - w * 0.2f, right = cx + w * 0.2f;
        float dot = w * 0.085f;
        c.drawCircle(left, row1, dot, line);                            // Back
        c.drawCircle(right, row1, dot, line);                           // Home
        c.drawCircle(left, row2, dot, line);
        // The lit one: the settings gear's place on most remotes.
        c.drawCircle(right, row2, dot * 1.9f, glow);
        c.drawCircle(right, row2, dot * 1.15f, lit);
        r.set(left - dot, y + h * 0.65f, left + dot, y + h * 0.86f);     // volume
        c.drawRoundRect(r, dot, dot, line);
        r.set(right - dot, y + h * 0.65f, right + dot, y + h * 0.86f);   // channel
        c.drawRoundRect(r, dot, dot, line);
    }

    @Override public void setAlpha(int a) { line.setAlpha(a); lit.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { line.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
