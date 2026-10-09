package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.mapper.AlertPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.AlertJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!memory")
@Transactional
public class AlertPersistenceAdapter implements AlertRepository {

    private final AlertJpaRepository alertJpaRepository;
    private final CageJpaRepository cageJpaRepository;
    private final GuineaPigJpaRepository guineaPigJpaRepository;
    private final AlertPersistenceMapper mapper;

    public AlertPersistenceAdapter(AlertJpaRepository alertJpaRepository, CageJpaRepository cageJpaRepository,
                                   GuineaPigJpaRepository guineaPigJpaRepository, AlertPersistenceMapper mapper) {
        this.alertJpaRepository = alertJpaRepository;
        this.cageJpaRepository = cageJpaRepository;
        this.guineaPigJpaRepository = guineaPigJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Alert save(Alert alert) {
        var cage = PersistenceEntityResolver.requireCage(cageJpaRepository, alert.getCageCode());
        GuineaPigJpaEntity guineaPig = alert.getGuineaPigId() == null
                ? null
                : PersistenceEntityResolver.requireGuineaPig(
                        guineaPigJpaRepository, alert.getGuineaPigId(), alert.getCageCode());
        return mapper.toDomain(alertJpaRepository.save(mapper.toEntity(alert, cage, guineaPig)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alert> findById(long id) {
        return alertJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findAll() {
        return alertJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findByStatus(AlertStatus status) {
        return alertJpaRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
