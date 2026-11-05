package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.AlertHistoryFilter;
import com.cuymonitor.backend.domain.model.AlertHistoryPage;
import com.cuymonitor.backend.domain.model.AlertHistoryQuery;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.Paging;
import com.cuymonitor.backend.domain.model.TimeRange;
import com.cuymonitor.backend.domain.port.in.CreateCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.GetAlertHistoryUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageWeightUseCase;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigHistoryUseCase;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigPhotoUseCase;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import com.cuymonitor.backend.domain.port.in.ListCagesUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.ReviewAlertUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateCageUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// The real subject limits the alert history to the requester's cages; the proxy only checked the access.
class CageOperationsServiceAlertHistoryTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final UUID ANA = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    private static final TimeRange DAY = TimeRange.of(null, NOW, NOW);

    private final List<AlertHistoryQuery> asked = new ArrayList<>();
    private CageOperationsService service;

    @BeforeEach
    void setUp() {
        ListCagesUseCase listCages = requester -> List.of(
                new Cage(1L, "cage-ana", "Jaula", null, ANA, true, NOW),
                new Cage(2L, "cage-ana-2", "Patio", null, ANA, true, NOW));
        GetAlertHistoryUseCase history = query -> {
            asked.add(query);
            return AlertHistoryPage.empty(query.paging(), query.range());
        };
        service = new CageOperationsService(new CageUseCases(listCages, mock(GetCageUseCase.class),
                mock(CreateCageUseCase.class), mock(UpdateCageUseCase.class), mock(DeleteCageUseCase.class),
                mock(GetCageHealthUseCase.class), mock(ListGuineaPigsUseCase.class),
                mock(RegisterGuineaPigUseCase.class), mock(DeleteGuineaPigUseCase.class),
                mock(GetGuineaPigPhotoUseCase.class), mock(GetGuineaPigHistoryUseCase.class),
                mock(GetCageWeightUseCase.class), mock(ListAlertsUseCase.class), mock(ReviewAlertUseCase.class),
                history));
    }

    private static AlertHistoryFilter filter(String cageCode) {
        return new AlertHistoryFilter(cageCode, null, null, null, null, DAY, Paging.of(null, null));
    }

    @Test
    void withoutACageItReadsEveryCageOfTheRequester() {
        service.getAlertHistory(ANA, filter(null));

        assertThat(asked.getFirst().cageCodes()).isEqualTo(Set.of("cage-ana", "cage-ana-2"));
    }

    @Test
    void withACageItReadsOnlyThatOne() {
        service.getAlertHistory(ANA, filter("cage-ana-2"));

        assertThat(asked.getFirst().cageCodes()).isEqualTo(Set.of("cage-ana-2"));
    }
}
