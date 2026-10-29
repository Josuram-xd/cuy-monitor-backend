package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.port.in.ListCatalogOptionsUseCase;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CatalogService implements ListCatalogOptionsUseCase {

    private final CatalogOptionRepository options;

    public CatalogService(CatalogOptionRepository options) {
        this.options = options;
    }

    /** A kind with no active row is simply missing from the map. */
    @Override
    @Transactional(readOnly = true)
    public Map<CatalogKind, List<CatalogOption>> list() {
        return options.findAllActive().stream()
                .collect(Collectors.groupingBy(CatalogOption::kind,
                        () -> new EnumMap<>(CatalogKind.class), Collectors.toList()));
    }
}
