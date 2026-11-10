package com.github.catvod.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CatalogRequestsTest {

    @Test
    public void catalogRequestsBypassAFreshHttpCache() throws Exception {
        MockWebServer server = new MockWebServer();
        String stale = "{\"list\":[{\"vod_name\":\"昨日旧片\",\"type_name\":\"电影\",\"vod_time\":\"2026-11-09\"}]}";
        String live = "{\"list\":[{\"vod_name\":\"今日新片\",\"type_name\":\"电影\",\"vod_time\":\"2026-11-10\"}]}";
        server.enqueue(new MockResponse().setBody(stale).setHeader("Cache-Control", "public, max-age=86400"));
        server.enqueue(new MockResponse().setBody(live).setHeader("Cache-Control", "public, max-age=86400"));
        server.start();
        File directory = Files.createTempDirectory("catalog-http").toFile();
        Cache cache = new Cache(directory, 1024 * 1024);
        OkHttpClient client = new OkHttpClient.Builder().cache(cache).build();
        List<String> log = new ArrayList<>();
        try {
            Request plain = new Request.Builder().url(server.url("/vod?ac=videolist&t=1&pg=1")).build();
            try (Response first = client.newCall(plain).execute()) {
                assertEquals(stale, first.body().string());
                log.add("first network -> " + stale);
            }
            try (Response second = client.newCall(plain).execute()) {
                assertEquals(stale, second.body().string());
                log.add("second from http cache -> " + stale + " network=" + second.networkResponse() + " cache=" + second.cacheResponse());
            }
            assertEquals(1, server.getRequestCount());
            Request catalog = CatalogRequests.apply(plain.newBuilder()).build();
            assertTrue(catalog.header("Cache-Control").contains("no-cache"));
            assertEquals("no-cache", catalog.header("Pragma"));
            try (Response third = client.newCall(catalog).execute()) {
                String body = third.body().string();
                assertEquals(live, body);
                log.add("catalog force network -> " + body);
            }
            assertEquals(2, server.getRequestCount());
            System.out.println(String.join("\n", log));
        } finally {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            cache.close();
            server.shutdown();
        }
    }

    @Test
    public void everyConfiguredCategoryIsRefreshed() {
        List<String> ids = CatalogCategories.typeIds(Arrays.asList("1", "", "home", "2", "4", "3", "36", "1", "46", null));
        assertEquals(List.of("1", "2", "4", "3", "36", "46"), ids);
    }
}
