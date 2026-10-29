package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;

import java.util.List;

public interface CatalogOptionRepository {
    /** Every active option of every kind, already in the order they should be shown. */
    List<CatalogOption> findAllActive();

    boolean existsActive(CatalogKind kind, String code);
}
