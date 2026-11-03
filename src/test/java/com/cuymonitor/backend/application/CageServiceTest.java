package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.in.CreateCageCommand;
import com.cuymonitor.backend.domain.port.in.UpdateCageCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CageServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T14:00:00Z");
    private static final UUID ANA = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    private static final UUID LUIS = UUID.fromString("00000000-0000-4000-8000-00000000000b");

    private InMemoryCageRepository cages;
    private CageService service;

    @BeforeEach
    void setUp() {
        cages = new InMemoryCageRepository();
        service = new CageService(cages, new MutableClock(NOW));
    }

    @Test
    void createsACageWithARandomCodeForItsOwner() {
        Cage created = service.create(new CreateCageCommand(ANA, "  Jaula del patio ", " Patio "));

        assertThat(created.code()).matches("cage-[0-9a-f]{8}");
        assertThat(created.name()).isEqualTo("Jaula del patio");
        assertThat(created.location()).isEqualTo("Patio");
        assertThat(created.ownerId()).isEqualTo(ANA);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.active()).isTrue();
    }

    @Test
    void twoCagesNeverShareACode() {
        Cage first = service.create(new CreateCageCommand(ANA, "Uno", null));
        Cage second = service.create(new CreateCageCommand(ANA, "Dos", null));

        assertThat(first.code()).isNotEqualTo(second.code());
    }

    @Test
    void listsOnlyTheCagesOfTheAccountOldestFirst() {
        service.create(new CreateCageCommand(ANA, "Primera", null));
        service.create(new CreateCageCommand(LUIS, "De Luis", null));
        service.create(new CreateCageCommand(ANA, "Segunda", null));

        assertThat(service.list(ANA)).extracting(Cage::name).containsExactly("Primera", "Segunda");
        assertThat(service.list(LUIS)).extracting(Cage::name).containsExactly("De Luis");
    }

    @Test
    void renamesAndClearsTheLocation() {
        Cage created = service.create(new CreateCageCommand(ANA, "Vieja", "Galpón"));

        Cage updated = service.update(new UpdateCageCommand(created.code(), "Nueva", "  "));

        assertThat(updated.name()).isEqualTo("Nueva");
        assertThat(updated.location()).isNull();
        assertThat(service.get(created.code()).name()).isEqualTo("Nueva");
    }

    @Test
    void aDeletedCageDisappearsButKeepsItsCode() {
        Cage created = service.create(new CreateCageCommand(ANA, "Temporal", null));

        service.delete(created.code());

        assertThat(service.list(ANA)).isEmpty();
        assertThatThrownBy(() -> service.get(created.code())).isInstanceOf(CageNotFoundException.class);
        assertThat(cages.existsByCode(created.code())).isTrue();
    }

    @Test
    void anUnknownCageIsNotFound() {
        assertThatThrownBy(() -> service.update(new UpdateCageCommand("cage-nope", "x", null)))
                .isInstanceOf(CageNotFoundException.class);
        assertThatThrownBy(() -> service.delete("cage-nope")).isInstanceOf(CageNotFoundException.class);
    }

    @Test
    void rejectsANameThatIsBlankOrTooLong() {
        assertThatThrownBy(() -> service.create(new CreateCageCommand(ANA, "   ", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(new CreateCageCommand(ANA, "x".repeat(101), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
