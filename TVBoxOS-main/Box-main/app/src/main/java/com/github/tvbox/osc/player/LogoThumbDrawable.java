package com.github.tvbox.osc.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.tvbox.osc.R;

/**
 * SeekBar thumb that uses the RanNuan logo and grows slightly when focused.
 */
public class LogoThumbDrawable extends Drawable {
    private final Drawable logo;
    private final int normalSize;
    private final int activeSize;
    private boolean active;

    public LogoThumbDrawable(Context context) {
        normalSize = dp(context, 24);
        activeSize = dp(context, 32);
        logo = ContextCompat.getDrawable(context, R.drawable.player_thumb_logo_bitmap);
    }

    public static void attach(SeekBar seekBar) {
        if (seekBar == null) {
            return;
        }
        LogoThumbDrawable thumb = new LogoThumbDrawable(seekBar.getContext());
        int half = Math.max(1, thumb.getIntrinsicWidth() / 2);
        seekBar.setThumb(thumb);
        seekBar.setThumbOffset(half);
        seekBar.setPadding(half, seekBar.getPaddingTop(), half, seekBar.getPaddingBottom());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            seekBar.setSplitTrack(false);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (logo == null) {
            return;
        }
        int size = active ? activeSize : normalSize;
        Rect bounds = getBounds();
        int left = Math.round(bounds.exactCenterX() - size / 2f);
        int top = Math.round(bounds.exactCenterY() - size / 2f);
        logo.setBounds(left, top, left + size, top + size);
        logo.draw(canvas);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean newActive = false;
        if (stateSet != null) {
            for (int state : stateSet) {
                if (state == android.R.attr.state_pressed
                        || state == android.R.attr.state_focused
                        || state == android.R.attr.state_selected) {
                    newActive = true;
                    break;
                }
            }
        }
        if (active == newActive) {
            return false;
        }
        active = newActive;
        invalidateSelf();
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        if (logo != null) {
            logo.setAlpha(alpha);
        }
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (logo != null) {
            logo.setColorFilter(colorFilter);
        }
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return activeSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return activeSize;
    }

    private static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
