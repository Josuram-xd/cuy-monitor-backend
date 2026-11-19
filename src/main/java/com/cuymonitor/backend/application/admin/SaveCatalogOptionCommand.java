package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;

import java.util.Objects;

/** Command — ConcreteCommand: adds a catalog option, or overwrites the one with the same kind and code. */
public final class SaveCatalogOptionCommand implements AdminCommand<CatalogOption> {

    private final CatalogOptionRepository catalog;
    private final CatalogOption option;

    public SaveCatalogOptionCommand(CatalogOptionRepository catalog, CatalogOption option) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.option = Objects.requireNonNull(option, "option");
    }

    @Override
    public CatalogOption execute() {
        if (option.code().length() > CatalogOption.MAX_CODE_LENGTH) {
            throw new InvalidAdminActionException(
                    "a code can have at most " + CatalogOption.MAX_CODE_LENGTH + " characters");
        }
        // the database only allows A-Z, 0-9 and _ (ck_catalog_option_code): say so here instead of failing on insert
        if (!option.code().matches("[A-Z0-9_]+")) {
            throw new InvalidAdminActionException("a code can only have letters, numbers and _");
        }
        if (option.hex() != null && !option.hex().matches("#[0-9a-f]{6}")) {
            throw new InvalidAdminActionException("a color must look like #a1b2c3, in lower case");
        }
        return catalog.save(option);
    }

    @Override
    public String description() {
        return "save " + option.kind() + " " + option.code();
    }
}
