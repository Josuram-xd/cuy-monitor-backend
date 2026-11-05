package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.AlertHistoryPage;

import java.time.Instant;
import java.util.List;

// Shape of docs/contracts/rest-api.md, 3.9: the alerts of the page with the shape of 3.5, and where the page is.
public record AlertHistoryResponse(List<AlertResponse> items, int page, int size, long totalItems, int totalPages,
                                   boolean hasNext, Instant from, Instant to) {

    public static AlertHistoryResponse from(AlertHistoryPage history) {
        return new AlertHistoryResponse(
                history.items().stream().map(AlertResponse::from).toList(),
                history.paging().page(),
                history.paging().size(),
                history.totalItems(),
                history.totalPages(),
                history.hasNext(),
                history.range().from(),
                history.range().to());
    }
}
