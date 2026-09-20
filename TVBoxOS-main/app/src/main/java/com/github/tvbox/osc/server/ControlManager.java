package com.github.tvbox.osc.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.receiver.PushReceiver;
import com.github.tvbox.osc.receiver.SearchReceiver;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @author pj567
 * @date :2021/1/4
 * @description:
 */
public class ControlManager {
    private static ControlManager instance;
    private RemoteServer mServer = null;
    public static Context mContext;

    private ControlManager() {

    }

    public static ControlManager get() {
        if (instance == null) {
            synchronized (ControlManager.class) {
                if (instance == null) {
                    instance = new ControlManager();
                }
            }
        }
        return instance;
    }

    public static void init(Context context) {
        mContext = context;
    }

    public String getAddress(boolean local) {
        if (mServer == null || !mServer.isStarting()) {
            startServer();
        }
        if (mServer == null || !mServer.isStarting()) {
            return "";
        }
        return local ? mServer.getLoadAddress() : mServer.getServerAddress();
    }

    public void startServer() {
        if (mServer != null && mServer.isStarting()) {
            return;
        }
        do {
            mServer = new RemoteServer(RemoteServer.serverPort, mContext);
            mServer.setDataReceiver(new DataReceiver() {
                @Override
                public void onTextReceived(String text) {
                    if (!TextUtils.isEmpty(text)) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("title", text);
                        intent.setAction(SearchReceiver.action);
                        intent.setPackage(mContext.getPackageName());
                        intent.setComponent(new ComponentName(mContext, SearchReceiver.class));
                        intent.putExtras(bundle);
                        mContext.sendBroadcast(intent);
                    }
                }

                @Override
                public void onApiReceived(String url) {
                    applyApiConfig(url);
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_API_URL_CHANGE, url));
                }

                @Override
                public void onLiveApiReceived(String url) {
                    if (!TextUtils.isEmpty(url)) {
                        Hawk.put(HawkConfig.LIVE_API_URL, url);
                        HistoryHelper.setLiveApiHistory(url);
                        reloadHomeForConfigChange();
                    }
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_LIVE_API_URL_CHANGE, url));
                }

                @Override
                public void onDanmuApiReceived(String url) {
                    Hawk.put(HawkConfig.DANMU_API, TextUtils.isEmpty(url) ? "" : url);
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SET_DANMU_SETTINGS, false));
                }

                @Override
                public void onPushReceived(String url) {
                    PushReceiver.send(mContext, url);
                }
            });
            try {
                mServer.start();
                com.github.catvod.Proxy.set(RemoteServer.serverPort);
                IjkMediaPlayer.setDotPort(Hawk.get(HawkConfig.DOH_URL, 0) > 0, RemoteServer.serverPort);
                break;
            } catch (IOException ex) {
                RemoteServer.serverPort++;
                mServer.stop();
            }
        } while (RemoteServer.serverPort < 9999);
    }

    private void applyApiConfig(String url) {
        if (TextUtils.isEmpty(url)) return;
        String api = url.trim();
        String oldApi = Hawk.get(HawkConfig.API_URL, "");
        if (TextUtils.equals(oldApi, api)) return;

        Hawk.put(HawkConfig.API_URL, api);
        Hawk.put(HawkConfig.LIVE_API_URL, api);
        HistoryHelper.setApiHistory(api);
        HistoryHelper.setLiveApiHistory(api);
        if (!HistoryHelper.isApiLineHistory(api)) {
            HistoryHelper.clearApiLineList();
        }
        SourceViewModel.clearRuntimeCache();
        reloadHomeForConfigChange();
    }

    private void reloadHomeForConfigChange() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mContext, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("useCache", false);
                mContext.startActivity(intent);
            }
        });
    }

    public void stopServer() {
        if (mServer != null && mServer.isStarting()) {
            mServer.stop();
        }
        mServer = null;
    }
}
