package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.FakeCatalogOptionRepository;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceTest {

    private final FakeCatalogOptionRepository options = new FakeCatalogOptionRepository();
    private final CatalogService service = new CatalogService(options);

    @Test
    void groupsTheOptionsByKindKeepingTheirOrder() {
        Map<CatalogKind, List<CatalogOption>> byKind = service.list();

        assertThat(byKind).containsOnlyKeys(CatalogKind.MARK_COLOR, CatalogKind.COAT_COLOR, CatalogKind.BREED);
        assertThat(byKind.get(CatalogKind.MARK_COLOR)).extracting(CatalogOption::code)
                .containsExactly("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "BLACK", "WHITE");
        assertThat(byKind.get(CatalogKind.BREED)).first()
                .satisfies(breed -> assertThat(breed.code()).isEqualTo("AMERICAN"));
    }

    @Test
    void aKindWithNoActiveOptionIsLeftOut() {
        for (String code : List.of("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "BLACK", "WHITE")) {
            options.retire(CatalogKind.MARK_COLOR, code);
        }

        assertThat(service.list()).doesNotContainKey(CatalogKind.MARK_COLOR);
    }
}
