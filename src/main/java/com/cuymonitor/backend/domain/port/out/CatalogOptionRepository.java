package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;

import java.util.List;
import java.util.Optional;

public interface CatalogOptionRepository {
    /** Every active option of every kind, already in the order they should be shown. */
    List<CatalogOption> findAllActive();

    boolean existsActive(CatalogKind kind, String code);

    /** Every option, active or not, in the order they are shown. Only the admin screen needs the inactive ones. */
    List<CatalogOption> findAll();

    Optional<CatalogOption> find(CatalogKind kind, String code);

    /** Creates the option or overwrites the one with the same kind and code. */
    CatalogOption save(CatalogOption option);
}
