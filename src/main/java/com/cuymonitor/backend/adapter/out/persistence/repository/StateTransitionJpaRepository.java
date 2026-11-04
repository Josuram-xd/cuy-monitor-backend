package com.cuymonitor.backend.adapter.out.persistence.repository;

import java.util.List;
import java.time.Instant;
import com.cuymonitor.backend.adapter.out.persistence.entity.StateTransitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateTransitionJpaRepository extends JpaRepository<StateTransitionJpaEntity, Long> {
    List<StateTransitionJpaEntity> findByGuineaPig_IdAndOccurredAtBetweenOrderByOccurredAtAsc(long guineaPigId,
                                                                                            Instant from, Instant to);
}
