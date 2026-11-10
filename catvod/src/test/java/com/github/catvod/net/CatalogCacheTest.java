package com.github.catvod.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class CatalogCacheTest {

    private static final String[] CATEGORIES = {"电影", "电视剧", "动漫", "综艺", "体育", "短剧"};

    @Test
    public void openingTheCatalogReplacesPersistedListsWithLiveOnes() throws Exception {
        Map<String, String> disk = new LinkedHashMap<>();
        AtomicLong now = new AtomicLong(1_762_000_000_000L);
        NetCache cache = new NetCache(() -> 0L, now::get, disk::get, (key, value) -> disk.put(key, value), key -> {
            if (key == null) disk.clear();
            else disk.remove(key);
        });
        List<String> log = new ArrayList<>();
        for (String category : CATEGORIES) {
            String stale = list(category, "昨日旧片", "2026-11-09");
            disk.put(category, "{\"time\":" + now.get() + ",\"value\":" + stale + "}");
            String served = cache.cached(category, new CacheOptions(86_400_000L, false, true), () -> {
                throw new AssertionError("fresh cache must not reload " + category);
            }).get();
            assertEquals(stale, served);
            log.add("cached " + category + " -> " + served);
        }
        cache.clear(null);
        assertEquals(0, disk.size());
        for (String category : CATEGORIES) {
            String live = list(category, "今日新片", "2026-11-10");
            String served = cache.cached(category, new CacheOptions(86_400_000L, false, true), () -> CompletableFuture.completedFuture(live)).get();
            assertEquals(live, served);
            assertFalse(disk.get(category).contains("昨日旧片"));
            log.add("live " + category + " -> " + served);
        }
        System.out.println(String.join("\n", log));
    }

    private static String list(String category, String title, String updated) {
        return "{\"list\":[{\"vod_name\":\"" + title + "\",\"type_name\":\"" + category + "\",\"vod_time\":\"" + updated + "\"}]}";
    }
}
