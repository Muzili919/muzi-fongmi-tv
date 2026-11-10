package com.fongmi.android.tv.api;

import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.CatalogCategories;
import com.github.catvod.utils.Task;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class CatalogRefresh {

    private static final AtomicInteger GENERATION = new AtomicInteger();

    private CatalogRefresh() {
    }

    public static void prefetch(Site site, Result home) {
        if (site == null || site.isEmpty() || home == null) return;
        int token = GENERATION.incrementAndGet();
        Map<String, Class> types = new LinkedHashMap<>();
        for (Class type : home.getTypes()) types.put(type.getTypeId(), type);
        List<String> ids = CatalogCategories.typeIds(types.keySet());
        String key = site.getKey();
        for (String id : ids) {
            Class type = types.get(id);
            HashMap<String, String> extend = initialExtend(type);
            String name = type.getTypeName();
            Task.submitLarge(() -> load(token, key, id, name, extend));
        }
    }

    private static HashMap<String, String> initialExtend(Class type) {
        HashMap<String, String> extend = new HashMap<>();
        for (Filter filter : type.getFilters()) if (filter.getInit() != null) extend.put(filter.getKey(), filter.getInit());
        return extend;
    }

    private static void load(int token, String key, String id, String name, HashMap<String, String> extend) {
        if (GENERATION.get() != token) return;
        try {
            Result result = SiteApi.categoryContent(key, id, "1", true, extend);
            if (GENERATION.get() != token) return;
            Vod first = result.getList().isEmpty() ? null : result.getList().get(0);
            SpiderDebug.log("catalog", "live category=%s id=%s count=%d first=%s remarks=%s year=%s", name, id, result.getList().size(), first == null ? "" : first.getName(), first == null ? "" : first.getRemarks(), first == null ? "" : first.getYear());
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }
}
