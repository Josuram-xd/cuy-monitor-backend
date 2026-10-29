package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;

import java.util.List;
import java.util.Map;

public interface ListCatalogOptionsUseCase {
    Map<CatalogKind, List<CatalogOption>> list();
}
