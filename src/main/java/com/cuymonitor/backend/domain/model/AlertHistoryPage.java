package com.cuymonitor.backend.domain.model;

import java.util.List;
import java.util.Objects;

// One page of the alert history, newest first, and how many alerts match in total.
public record AlertHistoryPage(List<Alert> items, Paging paging, long totalItems, TimeRange range) {

    public AlertHistoryPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        Objects.requireNonNull(paging, "paging");
        Objects.requireNonNull(range, "range");
        if (totalItems < items.size()) {
            throw new IllegalArgumentException("totalItems can't be less than the items of the page");
        }
    }

    public static AlertHistoryPage empty(Paging paging, TimeRange range) {
        return new AlertHistoryPage(List.of(), paging, 0, range);
    }

    public int totalPages() {
        return (int) ((totalItems + paging.size() - 1) / paging.size());
    }

    public boolean hasNext() {
        return paging.page() + 1 < totalPages();
    }
}
