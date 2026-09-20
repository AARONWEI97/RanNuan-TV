package com.github.tvbox.osc.ui.tv.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Desktop/mobile cinema opening atmosphere: projector beam, film holes,
 * floating theatre marks and a sliding load line. Logo/title sit in the layout.
 */
public class CinemaSplashView extends View {
    private static final String[] FLOAT_MARKS = {"🍿", "🥤", "🎞️", "📽️", "🍿", "🥤", "⭐"};
    private static final float[] FLOAT_X = {0.18f, 0.24f, 0.78f, 0.80f, 0.12f, 0.74f, 0.86f};
    private static final float[] FLOAT_Y = {0.16f, 0.58f, 0.24f, 0.72f, 0.46f, 0.64f, 0.10f};

    private final Paint beamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dustPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path beamPath = new Path();
    private final Path innerBeamPath = new Path();
    private final RectF holeRect = new RectF();
    private final RectF barTrack = new RectF();
    private final RectF barFill = new RectF();
    private final float[] dustX = new float[15];
    private final float[] dustStart = new float[15];
    private final float[] dustSpeed = new float[15];

    private ValueAnimator animator;
    private float phase;
    private boolean dustSeeded;

    public CinemaSplashView(Context context) {
        super(context);
        init();
    }

    public CinemaSplashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CinemaSplashView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        holePaint.setColor(0x33FBBF24);
        dustPaint.setColor(0x66FCD34D);
        barTrackPaint.setColor(0x1AFBBF24);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < dustX.length; i++) {
            dustSpeed[i] = 0.18f + (i % 5) * 0.04f;
            dustStart[i] = i / (float) dustX.length;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(2400);
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        dustSeeded = false;
        float cx = w / 2f;
        float bottom = h - dp(12);
        beamPath.reset();
        beamPath.moveTo(cx, bottom);
        beamPath.lineTo(cx - w * 0.28f, 0);
        beamPath.lineTo(cx + w * 0.28f, 0);
        beamPath.close();
        innerBeamPath.reset();
        innerBeamPath.moveTo(cx, bottom);
        innerBeamPath.lineTo(cx - w * 0.14f, h * 0.12f);
        innerBeamPath.lineTo(cx + w * 0.14f, h * 0.12f);
        innerBeamPath.close();
        beamPaint.setShader(new LinearGradient(
                cx, bottom, cx, 0,
                new int[]{0x33FBBF24, 0x14FBBF24, 0x00000000},
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP
        ));
        glowPaint.setShader(new RadialGradient(
                cx, bottom, dp(28),
                new int[]{0x88FDE68A, 0x00000000},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        smokePaint.setShader(new LinearGradient(
                0, h, 0, h - dp(140),
                new int[]{0x14FBBF24, 0x00000000},
                null,
                Shader.TileMode.CLAMP
        ));
        if (!dustSeeded) {
            for (int i = 0; i < dustX.length; i++) {
                dustX[i] = cx + (i - 7) * dp(10);
            }
            dustSeeded = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        canvas.drawColor(0xFF080810);

        float beamPulse = 0.55f + 0.30f * wave(phase, 0f);
        beamPaint.setAlpha((int) (255 * beamPulse));
        canvas.drawPath(beamPath, beamPaint);
        beamPaint.setAlpha((int) (180 * beamPulse));
        canvas.drawPath(innerBeamPath, beamPaint);

        float glowScale = 0.85f + 0.25f * wave(phase, 0.2f);
        canvas.save();
        canvas.scale(glowScale, glowScale, w / 2f, h - dp(12));
        canvas.drawCircle(w / 2f, h - dp(12), dp(18), glowPaint);
        canvas.restore();
        canvas.drawRect(0, h - dp(140), w, h, smokePaint);

        drawFilmHoles(canvas, dp(18), 0f);
        drawFilmHoles(canvas, w - dp(30), 0.35f);
        drawDust(canvas, w, h);
        drawFloatingMarks(canvas, w, h);
        drawLoadingBar(canvas, w, h);
    }

    private void drawFilmHoles(Canvas canvas, float x, float delay) {
        int count = 12;
        float holeW = dp(10);
        float holeH = dp(8);
        float top = dp(24);
        float gap = (getHeight() - top * 2 - holeH) / (count - 1f);
        for (int i = 0; i < count; i++) {
            float alpha = 0.28f + 0.42f * wave(phase, delay + i * 0.06f);
            holePaint.setAlpha((int) (255 * alpha));
            float y = top + i * gap;
            holeRect.set(x, y, x + holeW, y + holeH);
            canvas.drawRoundRect(holeRect, dp(2), dp(2), holePaint);
        }
    }

    private void drawDust(Canvas canvas, int w, int h) {
        float cx = w / 2f;
        for (int i = 0; i < dustX.length; i++) {
            float t = (dustStart[i] + phase * dustSpeed[i] * 2.4f) % 1f;
            float x = cx + (dustX[i] - cx) * 0.35f + (i % 3 - 1) * dp(6);
            float y = h - dp(16) - t * h * 0.72f;
            dustPaint.setAlpha((int) (180 * (t < 0.2f ? t / 0.2f : 1f - t) * 0.7f));
            canvas.drawCircle(x, y, dp(1.4f), dustPaint);
        }
    }

    private void drawFloatingMarks(Canvas canvas, int w, int h) {
        emojiPaint.setTextSize(dp(22));
        for (int i = 0; i < FLOAT_MARKS.length; i++) {
            float lift = (float) Math.sin((phase + i * 0.13f) * Math.PI * 2) * dp(10);
            float x = w * FLOAT_X[i];
            float y = h * FLOAT_Y[i] + lift;
            emojiPaint.setAlpha(i == FLOAT_MARKS.length - 1 ? 160 : 210);
            canvas.drawText(FLOAT_MARKS[i], x, y, emojiPaint);
        }
    }

    private void drawLoadingBar(Canvas canvas, int w, int h) {
        float barW = dp(180);
        float barH = dp(3);
        float left = (w - barW) / 2f;
        float top = h / 2f + dp(96);
        barTrack.set(left, top, left + barW, top + barH);
        canvas.drawRoundRect(barTrack, barH, barH, barTrackPaint);
        float slide = (phase * 2f) % 2f;
        if (slide > 1f) {
            slide = 2f - slide;
        }
        float fillW = barW * 0.34f;
        float fillLeft = left + (barW - fillW) * slide;
        barFill.set(fillLeft, top, fillLeft + fillW, top + barH);
        barPaint.setShader(new LinearGradient(
                fillLeft, top, fillLeft + fillW, top,
                new int[]{0x00FBBF24, 0x99FBBF24, 0x00FBBF24},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(barFill, barH, barH, barPaint);
    }

    private float wave(float value, float offset) {
        return 0.5f + 0.5f * (float) Math.sin((value + offset) * Math.PI * 2);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
