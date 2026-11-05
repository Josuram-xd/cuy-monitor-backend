package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.entity.AlertJpaEntity;
import com.cuymonitor.backend.domain.model.AlertHistoryQuery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// Pure Fabrication: turns an AlertHistoryQuery into the JPA criteria and the page, so neither the domain
// nor the adapter has to know the other's language. Only the criteria that were given become a WHERE.
final class AlertHistorySpecification {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private AlertHistorySpecification() {
    }

    static Specification<AlertJpaEntity> of(AlertHistoryQuery query) {
        return (root, criteria, builder) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(root.get("cage").get("code").in(query.cageCodes()));
            where.add(builder.between(root.get("createdAt"), query.range().from(), query.range().to()));
            if (query.guineaPigId() != null) {
                where.add(builder.equal(root.get("guineaPig").get("id"), query.guineaPigId()));
            }
            if (query.level() != null) {
                where.add(builder.equal(root.get("level"), query.level()));
            }
            if (query.type() != null) {
                where.add(builder.equal(root.get("type"), query.type()));
            }
            if (query.status() != null) {
                where.add(builder.equal(root.get("status"), query.status()));
            }
            return builder.and(where.toArray(Predicate[]::new));
        };
    }

    static Pageable pageOf(AlertHistoryQuery query) {
        return PageRequest.of(query.paging().page(), query.paging().size(), NEWEST_FIRST);
    }
}
