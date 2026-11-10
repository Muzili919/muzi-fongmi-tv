package com.github.catvod.net;

import okhttp3.CacheControl;
import okhttp3.Request;

public final class CatalogRequests {

    private CatalogRequests() {
    }

    public static Request.Builder apply(Request.Builder builder) {
        return builder.cacheControl(CacheControl.FORCE_NETWORK).header("Pragma", "no-cache");
    }
}
