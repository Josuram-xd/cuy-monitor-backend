package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.integration.TestPostgres;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.cuymonitor.backend.adapter.out.persistence.mapper.BaselineProfilePersistenceMapper;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Against the real Postgres (V13 adds sample_count): a learned profile is inserted the first time and updated
 * in place afterwards, one row per guinea pig. Each test rolls back.
 */
@DataJpaTest(properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("com.cuymonitor.backend.integration.TestPostgres#canRun")
@Import({BaselineProfilePersistenceAdapter.class, BaselineProfilePersistenceMapper.class})
class BaselineProfilePersistenceTest {

    // Task 4.8: a throwaway Postgres with the cuy-monitor-db migrations, not a database on localhost
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestPostgres.register(registry);
    }

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private BaselineProfilePersistenceAdapter baselines;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    private long guineaPigId;

    @BeforeEach
    void createGuineaPig() {
        String code = "it-" + UUID.randomUUID().toString().substring(0, 8);
        long cageId = jdbc.queryForObject("insert into cage (name, code) values ('IT', ?) returning id", Long.class, code);
        guineaPigId = jdbc.queryForObject(
                "insert into guinea_pig (cage_id, mark_color) values (?, 'RED') returning id", Long.class, cageId);
    }

    @Test
    void aLearnedProfileIsSavedThenUpdatedInPlace() {
        baselines.save(new BaselineProfile(guineaPigId, 12.5, 2, 0.25, NOW, 1));
        entityManager.flush();
        baselines.save(new BaselineProfile(guineaPigId, 14.25, 3, 0.3125, NOW.plusSeconds(60), 2));
        // read what is in the table, not what Hibernate still has in memory
        entityManager.flush();
        entityManager.clear();

        assertThat(baselines.findByGuineaPigId(guineaPigId))
                .contains(new BaselineProfile(guineaPigId, 14.25, 3, 0.3125, NOW.plusSeconds(60), 2));
        assertThat(jdbc.queryForObject("select count(*) from baseline_profile where guinea_pig_id = ?",
                Integer.class, guineaPigId)).isEqualTo(1);
    }
}
