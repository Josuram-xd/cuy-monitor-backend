package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** An option as the admin panel sends it. The code is uppercased by the domain, so "colombiano" is fine. */
public record AdminCatalogOptionRequest(
        @NotNull CatalogKind kind,
        @NotBlank @Size(max = CatalogOption.MAX_CODE_LENGTH) String code,
        @NotBlank @Size(max = 60) String label,
        @Size(min = 7, max = 7) String hex,
        int sortOrder,
        Boolean active) {

    public CatalogOption toOption() {
        // a new option is on unless the panel says otherwise
        return new CatalogOption(kind, code, label, blankToNull(hex), sortOrder, active == null || active);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }
}
