package com.ansub.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

// Apple's liquid glass, as far as this TV can do it. Its GPU has no window blur, so glass over the home
// screen shows a blurred copy of the page taken when it opens (MainActivity.backdrop), never a live
// one: nothing to redraw each frame. On top: a tint, a gloss on the top half, and a rim that catches
// the light at the top left and fades round to the bottom right. Colours from Style.
// Every layer is a rounded rectangle with a shader, never a clip: a clip path costs the GPU on every
// frame the page moves, and the dock is on screen all the time.
final class LiquidGlass extends Drawable {
    private final float radius, rim;
    private final Bitmap backdrop; // the blurred page, or null (no copy: over another app, or the dock)
    private final Rect from;       // the part of backdrop under this glass, in its pixels
    private final Paint image = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG); // tint and gloss in one pass
    private final int tint;
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF(), ring = new RectF();

    LiquidGlass(float radius, float rim, int tint, Bitmap backdrop, Rect from) {
        this.radius = radius;
        this.rim = rim;
        this.backdrop = backdrop;
        this.from = from;
        this.tint = tint;
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(rim);
    }

    @Override
    protected void onBoundsChange(Rect b) {
        box.set(b);
        float in = rim / 2f;
        ring.set(b.left + in, b.top + in, b.right - in, b.bottom - in);
        if (backdrop != null && from != null && from.width() > 0 && from.height() > 0) {
            // The copy's part under the glass, stretched over the glass.
            BitmapShader s = new BitmapShader(backdrop, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Matrix m = new Matrix();
            m.setRectToRect(new RectF(from), box, Matrix.ScaleToFit.FILL);
            s.setLocalMatrix(m);
            image.setShader(s);
        }
        // The tint with the gloss over it at the top, plain tint from the middle down: one gradient, so
        // the glass is filled once a frame, not twice (the dock is big and always on screen).
        fill.setShader(new LinearGradient(0, b.top, 0, b.top + b.height() / 2f,
                over(Style.LIQUID_GLOSS, tint), tint, Shader.TileMode.CLAMP));
        edge.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[] {Style.LIQUID_RIM_HI, Style.LIQUID_RIM_LO, Style.LIQUID_RIM_END},
                new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas c) {
        if (image.getShader() != null) c.drawRoundRect(box, radius, radius, image);
        c.drawRoundRect(box, radius, radius, fill);
        c.drawRoundRect(ring, radius - rim / 2f, radius - rim / 2f, edge);
    }

    // top drawn over bottom, both see-through: the colour and alpha of the two together.
    private static int over(int top, int bottom) {
        float ta = (top >>> 24) / 255f, ba = (bottom >>> 24) / 255f, a = ta + ba * (1 - ta);
        if (a <= 0) return 0;
        int r = Math.round((((top >> 16) & 0xFF) * ta + ((bottom >> 16) & 0xFF) * ba * (1 - ta)) / a);
        int g = Math.round((((top >> 8) & 0xFF) * ta + ((bottom >> 8) & 0xFF) * ba * (1 - ta)) / a);
        int bl = Math.round(((top & 0xFF) * ta + (bottom & 0xFF) * ba * (1 - ta)) / a);
        return Math.round(a * 255) << 24 | r << 16 | g << 8 | bl;
    }

    @Override public void setAlpha(int a) { fill.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { fill.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
