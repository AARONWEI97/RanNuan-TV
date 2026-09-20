package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;

/**
 * Cold-start cinema opening. HomeActivity keeps its own data loading state
 * and is intentionally not used as the launcher for this animation.
 */
public class StartupActivity extends BaseActivity {
    private static final long OPENING_DURATION_MS = 3100L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openHome = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            Intent intent = new Intent(StartupActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    };

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_startup;
    }

    @Override
    protected boolean applyWallpaper() {
        return false;
    }

    @Override
    protected void init() {
        try {
            View logo = findViewById(R.id.splashLogo);
            View title = findViewById(R.id.splashTitle);
            View tagline = findViewById(R.id.splashTagline);
            if (logo != null) {
                logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_logo_reveal));
            }
            if (title != null) {
                title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_text_reveal));
            }
            if (tagline != null) {
                tagline.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_text_reveal));
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(openHome);
        handler.postDelayed(openHome, OPENING_DURATION_MS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(openHome);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        handler.removeCallbacks(openHome);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }
}
