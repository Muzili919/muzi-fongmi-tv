package com.github.catvod.net;

import java.util.LinkedHashSet;
import java.util.List;

public final class CatalogCategories {

    private CatalogCategories() {
    }

    public static List<String> typeIds(Iterable<String> typeIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (typeIds == null) return List.of();
        for (String id : typeIds) {
            if (id == null || id.isBlank() || "home".equals(id)) continue;
            ids.add(id);
        }
        return List.copyOf(ids);
    }
}
