package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;

import java.util.List;
import java.util.Map;

/** Shape in docs/contracts/rest-api.md, 3.6. hex is null for a breed, which has no color sample. */
public record CatalogResponse(List<Option> markColors, List<Option> coatColors, List<Option> breeds) {

    public record Option(String code, String label, String hex) {
    }

    public static CatalogResponse from(Map<CatalogKind, List<CatalogOption>> byKind) {
        return new CatalogResponse(options(byKind, CatalogKind.MARK_COLOR), options(byKind, CatalogKind.COAT_COLOR),
                options(byKind, CatalogKind.BREED));
    }

    private static List<Option> options(Map<CatalogKind, List<CatalogOption>> byKind, CatalogKind kind) {
        return byKind.getOrDefault(kind, List.of()).stream()
                .map(option -> new Option(option.code(), option.label(), option.hex()))
                .toList();
    }
}
