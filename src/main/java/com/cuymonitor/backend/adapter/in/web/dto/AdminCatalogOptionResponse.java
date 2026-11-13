package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;

/** Unlike the breeder's catalog, the admin one also shows the options that are switched off. */
public record AdminCatalogOptionResponse(CatalogKind kind, String code, String label, String hex, int sortOrder,
                                         boolean active) {

    public static AdminCatalogOptionResponse from(CatalogOption option) {
        return new AdminCatalogOptionResponse(option.kind(), option.code(), option.label(), option.hex(),
                option.sortOrder(), option.active());
    }
}
