package com.cuymonitor.backend.domain.exception;

import com.cuymonitor.backend.domain.model.CatalogKind;

import java.util.Locale;

/** The request used a code that is not an active row of catalog_option. */
public class UnknownCatalogOptionException extends RuntimeException {

    private final CatalogKind kind;

    public UnknownCatalogOptionException(CatalogKind kind, String code) {
        super("unknown " + kind.name().toLowerCase(Locale.ROOT) + ": " + code);
        this.kind = kind;
    }

    public CatalogKind getKind() {
        return kind;
    }
}
