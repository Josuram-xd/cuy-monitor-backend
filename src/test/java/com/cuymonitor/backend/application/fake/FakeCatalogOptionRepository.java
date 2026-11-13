package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The rows migration V10 of cuy-monitor-db inserts, so a test sees the same codes as a real database. */
public class FakeCatalogOptionRepository implements CatalogOptionRepository {

    private final List<CatalogOption> options = new ArrayList<>();

    public FakeCatalogOptionRepository() {
        String[] markColors = {"RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "BLACK", "WHITE"};
        String[] coatColors = {"WHITE", "BLACK", "BROWN", "CREAM", "GRAY", "CINNAMON", "BICOLOR", "TRICOLOR"};
        String[] breeds = {"AMERICAN", "PERUVIAN", "ABYSSINIAN", "TEDDY", "SILKIE", "SKINNY", "CRESTED", "OTHER"};
        add(CatalogKind.MARK_COLOR, markColors);
        add(CatalogKind.COAT_COLOR, coatColors);
        add(CatalogKind.BREED, breeds);
    }

    /** Takes a code out of the catalog, the way `active = false` does in the database. */
    public void retire(CatalogKind kind, String code) {
        options.removeIf(option -> option.kind() == kind && option.code().equals(code));
    }

    @Override
    public List<CatalogOption> findAll() {
        // this fake only holds options that are on, so "all" and "active" are the same list here
        return List.copyOf(options);
    }

    @Override
    public Optional<CatalogOption> find(CatalogKind kind, String code) {
        String normalized = CatalogOption.normalize(code);
        return options.stream()
                .filter(option -> option.kind() == kind && option.code().equals(normalized))
                .findFirst();
    }

    @Override
    public CatalogOption save(CatalogOption option) {
        options.removeIf(o -> o.kind() == option.kind() && o.code().equals(option.code()));
        if (option.active()) {
            options.add(option);
        }
        return option;
    }

    @Override
    public List<CatalogOption> findAllActive() {
        return List.copyOf(options);
    }

    @Override
    public boolean existsActive(CatalogKind kind, String code) {
        String normalized = CatalogOption.normalize(code);
        return options.stream().anyMatch(o -> o.kind() == kind && o.code().equals(normalized));
    }

    private void add(CatalogKind kind, String[] codes) {
        for (int i = 0; i < codes.length; i++) {
            options.add(CatalogOption.active(kind, codes[i], codes[i].charAt(0) + codes[i].substring(1).toLowerCase(),
                    kind == CatalogKind.BREED ? null : "#000000", i + 1));
        }
    }
}
