package com.github.tvbox.osc.util;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

public class SearchHelper {

    public static HashMap<String, String> getSourcesForSearch() {
        HashMap<String, String> mCheckSources;
        try {
            String api = Hawk.get(HawkConfig.API_URL, "");
            if(api.isEmpty())return null;
            HashMap<String, HashMap<String, String>> mCheckSourcesForApi = Hawk.get(HawkConfig.SOURCES_FOR_SEARCH, new HashMap<>());
            mCheckSources = mCheckSourcesForApi.get(api);
            HashMap<String, String> available = getSources();
            if (mCheckSources != null && !mCheckSources.isEmpty()) {
                String fingerprint = sourceFingerprint(available);
                HashMap<String, String> fingerprints = Hawk.get(HawkConfig.SOURCES_FOR_SEARCH_KEYS, new HashMap<>());
                String previousFingerprint = fingerprints.get(api);
                // A saved selection from an older version/config can contain only the
                // two CMS sources, while the current config now exposes many Spider
                // sources. Reset that stale selection once so all sources participate.
                if (previousFingerprint == null && available.size() > mCheckSources.size() + 2
                        && mCheckSources.size() <= 2) {
                    mCheckSources = available;
                    mCheckSourcesForApi.remove(api);
                    fingerprints.put(api, fingerprint);
                    Hawk.put(HawkConfig.SOURCES_FOR_SEARCH_KEYS, fingerprints);
                } else if (previousFingerprint != null && !fingerprint.equals(previousFingerprint)) {
                    // The API kept the same URL but changed its site list. Drop stale
                    // keys and start with the complete current list.
                    mCheckSources = available;
                    mCheckSourcesForApi.remove(api);
                    fingerprints.put(api, fingerprint);
                    Hawk.put(HawkConfig.SOURCES_FOR_SEARCH_KEYS, fingerprints);
                }
            }
        } catch (Exception e) {
            return null;
        }
        if (mCheckSources == null || mCheckSources.isEmpty()) {
            mCheckSources = getSources();
        }
//        else {
//            HashMap<String, String> newSources = getSources();
//            for (Map.Entry<String, String> entry : newSources.entrySet()) {
//                String newKey = entry.getKey();
//                String newValue = entry.getValue();
//                if (!mCheckSources.containsKey(newKey)) {
//                    mCheckSources.put(newKey, newValue);
//                }
//            }
//            Iterator<Map.Entry<String, String>> iterator = mCheckSources.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<String, String> oldEntry = iterator.next();
//                String oldKey = oldEntry.getKey();
//                if (!newSources.containsKey(oldKey)) {
//                    iterator.remove();
//                }
//            }
//        }
        return mCheckSources;
    }

    public static void putCheckedSources(HashMap<String, String> mCheckSources,boolean isAll) {
        String api = Hawk.get(HawkConfig.API_URL, "");
        if (api.isEmpty()) {
            return;
        }
        HashMap<String, HashMap<String, String>> mCheckSourcesForApi = Hawk.get(HawkConfig.SOURCES_FOR_SEARCH,null);

        if(isAll){
            if (mCheckSourcesForApi == null) return;
            if (mCheckSourcesForApi.containsKey(api)) mCheckSourcesForApi.remove(api);
        }else {
            if (mCheckSourcesForApi == null) mCheckSourcesForApi = new HashMap<>();
            mCheckSourcesForApi.put(api, mCheckSources);
        }
        HashMap<String, String> fingerprints = Hawk.get(HawkConfig.SOURCES_FOR_SEARCH_KEYS, new HashMap<>());
        fingerprints.put(api, sourceFingerprint(getSources()));
        Hawk.put(HawkConfig.SOURCES_FOR_SEARCH_KEYS, fingerprints);
        SearchActivity.setCheckedSourcesForSearch(mCheckSources);
        Hawk.put(HawkConfig.SOURCES_FOR_SEARCH, mCheckSourcesForApi);
    }

    private static String sourceFingerprint(HashMap<String, String> sources) {
        ArrayList<String> keys = new ArrayList<>();
        if (sources != null) keys.addAll(sources.keySet());
        Collections.sort(keys);
        return android.text.TextUtils.join("\u0001", keys);
    }

    public static HashMap<String, String> getSources(){
        HashMap<String, String> mCheckSources = new HashMap<>();
        for (SourceBean bean : ApiConfig.get().getSourceBeanList()) {
            if (!bean.isSearchable()) {
                continue;
            }
            mCheckSources.put(bean.getKey(), "1");
        }
        return mCheckSources;
    }

    public static List<String> splitWords(String text) {
        List<String> result = new ArrayList<>();
        result.add(text);
        String[] parts = text.split("\\W+");
        if (parts.length > 1) {
            result.addAll(Arrays.asList(parts));
        }
        return result;
    }

}
