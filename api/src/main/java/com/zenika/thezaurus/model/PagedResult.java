package com.zenika.thezaurus.model;

import java.util.Collections;
import java.util.List;

public record PagedResult<T>(List<T> items, long total, int page, int pageSize, int totalPages) {

    public static <T> PagedResult<T> of(List<T> allItems, int page, int pageSize) {
        if (allItems == null) {
            return new PagedResult<>(Collections.emptyList(), 0, page, pageSize, 0);
        }
        long total = allItems.size();
        if (pageSize <= 0) {
            return new PagedResult<>(List.copyOf(allItems), total, 0, (int) total, total > 0 ? 1 : 0);
        }
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int safePage = Math.max(0, page);
        int fromIndex = safePage * pageSize;
        if (fromIndex >= total) {
            return new PagedResult<>(Collections.emptyList(), total, safePage, pageSize, totalPages);
        }
        int toIndex = Math.min(fromIndex + pageSize, (int) total);
        List<T> subList = List.copyOf(allItems.subList(fromIndex, toIndex));
        return new PagedResult<>(subList, total, safePage, pageSize, totalPages);
    }
}
