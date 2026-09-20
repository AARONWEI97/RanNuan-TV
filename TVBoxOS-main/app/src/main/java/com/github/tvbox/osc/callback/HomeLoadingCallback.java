package com.github.tvbox.osc.callback;

import com.github.tvbox.osc.R;
import com.kingja.loadsir.callback.Callback;

/**
 * Home-only loading state. It must stay separate from the app opening so a
 * refresh never replays the full cinema splash animation.
 */
public class HomeLoadingCallback extends Callback {
    @Override
    protected int onCreateView() {
        return R.layout.home_loading_layout;
    }
}
