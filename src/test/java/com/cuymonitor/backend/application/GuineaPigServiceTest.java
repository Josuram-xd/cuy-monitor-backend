package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryGuineaPigRepository;
import com.cuymonitor.backend.application.fake.FakeCatalogOptionRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.ColorAlreadyUsedException;
import com.cuymonitor.backend.domain.exception.UnknownCatalogOptionException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuineaPigServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");

    private InMemoryGuineaPigRepository guineaPigs;
    private FakeCatalogOptionRepository catalogOptions;
    private GuineaPigService service;

    @BeforeEach
    void setUp() {
        guineaPigs = new InMemoryGuineaPigRepository();
        catalogOptions = new FakeCatalogOptionRepository();
        service = new GuineaPigService(new InMemoryCageRepository(), guineaPigs, catalogOptions,
                new MutableClock(NOW));
    }

    @Test
    void registersANormalGuineaPigWithATrimmedName() {
        GuineaPig created = service.register(new RegisterGuineaPigCommand("cage-1", "  Canela ", "RED"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Canela");
        assertThat(created.getStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(created.getStatusSince()).isEqualTo(NOW);
        assertThat(service.list("cage-1")).extracting(GuineaPig::getName).containsExactly("Canela");
    }

    @Test
    void keepsTheBreedCoatWeightAndNotes() {
        GuineaPigProfile profile = new GuineaPigProfile("PERUVIAN", "TRICOLOR", 900, "nueva");

        GuineaPig created = service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "RED", profile));

        assertThat(service.list("cage-1")).singleElement().satisfies(pig -> {
            assertThat(pig.getId()).isEqualTo(created.getId());
            assertThat(pig.getProfile()).isEqualTo(profile);
        });
    }

    @Test
    void refusesAMarkColorThatIsNotInTheCatalog() {
        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "FUCHSIA")))
                .isInstanceOf(UnknownCatalogOptionException.class)
                .hasMessageContaining("FUCHSIA");

        assertThat(service.list("cage-1")).isEmpty();
    }

    @Test
    void refusesABreedOrCoatColorThatIsNotInTheCatalog() {
        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "RED",
                new GuineaPigProfile("DRAGON", null, null, null))))
                .isInstanceOf(UnknownCatalogOptionException.class);

        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "RED",
                new GuineaPigProfile(null, "NEON", null, null))))
                .isInstanceOf(UnknownCatalogOptionException.class);
    }

    @Test
    void aColorRetiredInTheCatalogStopsBeingAccepted() {
        catalogOptions.retire(CatalogKind.MARK_COLOR, "PURPLE");

        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("cage-1", "Lila", "PURPLE")))
                .isInstanceOf(UnknownCatalogOptionException.class);
    }

    @Test
    void deletingKeepsTheRecordButHidesItAndFreesItsColor() {
        GuineaPig canela = service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "RED"));

        service.delete("cage-1", canela.getId());

        assertThat(service.list("cage-1")).isEmpty();
        assertThat(guineaPigs.findById(canela.getId())).get().extracting(GuineaPig::isActive).isEqualTo(false);
        // the color is free again
        assertThat(service.register(new RegisterGuineaPigCommand("cage-1", "Pelusa", "RED")).getId())
                .isNotEqualTo(canela.getId());
    }

    @Test
    void deletingAnUnknownOrAlreadyDeletedGuineaPigIsNotFound() {
        GuineaPig canela = service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "RED"));
        service.delete("cage-1", canela.getId());

        assertThatThrownBy(() -> service.delete("cage-1", canela.getId()))
                .isInstanceOf(GuineaPigNotFoundException.class);
        assertThatThrownBy(() -> service.delete("cage-1", 999L)).isInstanceOf(GuineaPigNotFoundException.class);
    }

    @Test
    void deletingAnIdOfAnotherCageDoesNothing() {
        GuineaPig other = guineaPigs.save(GuineaPig.register("cage-2", "Ajeno", "BLUE", NOW));

        assertThatThrownBy(() -> service.delete("cage-1", other.getId()))
                .isInstanceOf(GuineaPigNotFoundException.class);
        assertThat(guineaPigs.findById(other.getId())).get().extracting(GuineaPig::isActive).isEqualTo(true);
    }

    @Test
    void deletingFromAnUnknownCageIsNotFound() {
        assertThatThrownBy(() -> service.delete("nope", 1L)).isInstanceOf(CageNotFoundException.class);
    }

    @Test
    void refusesAColorAnotherGuineaPigWears() {
        service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "RED"));

        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("cage-1", "Pelusa", "RED")))
                .isInstanceOf(ColorAlreadyUsedException.class);
        assertThat(service.list("cage-1")).hasSize(1);
    }

    @Test
    void differentColorsCanShareTheCage() {
        service.register(new RegisterGuineaPigCommand("cage-1", "Canela", "RED"));
        service.register(new RegisterGuineaPigCommand("cage-1", "Pelusa", "BLUE"));

        assertThat(service.list("cage-1")).extracting(GuineaPig::getName).containsExactly("Canela", "Pelusa");
    }

    @Test
    void anUnknownCageIsNotFound() {
        assertThatThrownBy(() -> service.register(new RegisterGuineaPigCommand("nope", "Canela", "RED")))
                .isInstanceOf(CageNotFoundException.class);
        assertThatThrownBy(() -> service.list("nope")).isInstanceOf(CageNotFoundException.class);
        assertThat(guineaPigs.findActiveByCage("nope")).isEmpty();
    }

    @Test
    void anEmptyCageHasNoGuineaPigs() {
        assertThat(service.list("cage-1")).isEmpty();
    }
}
