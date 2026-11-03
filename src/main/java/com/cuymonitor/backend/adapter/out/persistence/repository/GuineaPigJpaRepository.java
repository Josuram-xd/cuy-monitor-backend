package com.cuymonitor.backend.adapter.out.persistence.repository;

import com.cuymonitor.backend.adapter.out.persistence.GuineaPigRow;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuineaPigJpaRepository extends JpaRepository<GuineaPigJpaEntity, Long> {

    @Query("""
            select new com.cuymonitor.backend.adapter.out.persistence.GuineaPigRow(
                g.id, c.code, g.photoContentType, g.markColor, g.status, g.statusSince, g.active, g.createdAt,
                g.breed, g.coatColor, g.initialWeightGrams, g.notes)
            from GuineaPigJpaEntity g join g.cage c
            where c.code = :cageCode and g.active = true
            order by g.id asc""")
    List<GuineaPigRow> findActiveRowsByCage(@Param("cageCode") String cageCode);

    @Query("""
            select new com.cuymonitor.backend.adapter.out.persistence.GuineaPigRow(
                g.id, c.code, g.photoContentType, g.markColor, g.status, g.statusSince, g.active, g.createdAt,
                g.breed, g.coatColor, g.initialWeightGrams, g.notes)
            from GuineaPigJpaEntity g join g.cage c
            where c.code = :cageCode and g.markColor = :markColor and g.active = true""")
    Optional<GuineaPigRow> findActiveRowByCageAndColor(@Param("cageCode") String cageCode,
                                                       @Param("markColor") String markColor);

    @Query("""
            select new com.cuymonitor.backend.adapter.out.persistence.GuineaPigRow(
                g.id, c.code, g.photoContentType, g.markColor, g.status, g.statusSince, g.active, g.createdAt,
                g.breed, g.coatColor, g.initialWeightGrams, g.notes)
            from GuineaPigJpaEntity g join g.cage c
            where g.id = :id""")
    Optional<GuineaPigRow> findRowById(@Param("id") long id);
}
