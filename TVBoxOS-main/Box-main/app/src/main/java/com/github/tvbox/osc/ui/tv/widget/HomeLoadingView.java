package com.github.tvbox.osc.ui.tv.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Lightweight loading panel: a quiet glass card and one flowing progress line.
 * It has no splash artwork or timing of its own.
 */
public class HomeLoadingView extends View {
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panelRect = new RectF();
    private final RectF lineRect = new RectF();
    private ValueAnimator animator;
    private float phase;

    public HomeLoadingView(Context context) {
        super(context);
        init();
    }

    public HomeLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomeLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        panelPaint.setColor(0xB81B1E26);
        panelStrokePaint.setStyle(Paint.Style.STROKE);
        panelStrokePaint.setStrokeWidth(dp(1));
        panelStrokePaint.setColor(0x45FFFFFF);
        lineTrackPaint.setColor(0x2DFFFFFF);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1800L);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    phase = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
        }
        if (!animator.isStarted()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float panelWidth = Math.min(width - dp(48), dp(430));
        float panelHeight = dp(188);
        float left = (width - panelWidth) / 2f;
        float top = (height - panelHeight) / 2f;
        panelRect.set(left, top, left + panelWidth, top + panelHeight);
        canvas.drawRoundRect(panelRect, dp(22), dp(22), panelPaint);
        canvas.drawRoundRect(panelRect, dp(22), dp(22), panelStrokePaint);

        float centerX = width / 2f;
        float lineWidth = Math.min(panelWidth - dp(72), dp(280));
        float lineLeft = centerX - lineWidth / 2f;
        float lineTop = top + dp(151);
        lineRect.set(lineLeft, lineTop, lineLeft + lineWidth, lineTop + dp(3));
        canvas.drawRoundRect(lineRect, dp(2), dp(2), lineTrackPaint);
        float fillWidth = lineWidth * 0.28f;
        float fillLeft = lineLeft + ((phase * 1.8f) % 1f) * (lineWidth - fillWidth);
        linePaint.setShader(new LinearGradient(fillLeft, lineTop, fillLeft + fillWidth, lineTop,
                new int[]{0x00F97316, 0xFFFDBA74, 0x00F97316}, null, Shader.TileMode.CLAMP));
        lineRect.set(fillLeft, lineTop, fillLeft + fillWidth, lineTop + dp(3));
        canvas.drawRoundRect(lineRect, dp(2), dp(2), linePaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
