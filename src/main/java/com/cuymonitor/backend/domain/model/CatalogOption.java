package com.cuymonitor.backend.domain.model;

import java.util.Locale;
import java.util.Objects;

/** One row of catalog_option: what gets stored (code), what the breeder reads (label) and its dot color. */
public record CatalogOption(CatalogKind kind, String code, String label, String hex, int sortOrder) {

    /** Same length as the catalog_option.code column. */
    public static final int MAX_CODE_LENGTH = 20;

    public CatalogOption {
        Objects.requireNonNull(kind, "kind");
        code = normalize(code);
        if (code == null) {
            throw new IllegalArgumentException("code is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        label = label.trim();
    }

    /** Codes are stored upper case, so "red" and " RED " mean the same option. */
    public static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
