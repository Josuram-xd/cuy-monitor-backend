package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;

import java.util.Objects;

/** Command — ConcreteCommand: switches a catalog option on or off (never deletes it). */
public final class SetCatalogOptionActiveCommand implements AdminCommand<CatalogOption> {

    private final CatalogOptionRepository catalog;
    private final CatalogKind kind;
    private final String code;
    private final boolean active;

    public SetCatalogOptionActiveCommand(CatalogOptionRepository catalog, CatalogKind kind, String code,
                                         boolean active) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.code = CatalogOption.normalize(code);
        this.active = active;
    }

    @Override
    public CatalogOption execute() {
        CatalogOption option = catalog.find(kind, code)
                .orElseThrow(() -> new InvalidAdminActionException("there is no " + kind + " called " + code));
        return catalog.save(option.withActive(active));
    }

    @Override
    public String description() {
        return (active ? "switch on " : "switch off ") + kind + " " + code;
    }
}
