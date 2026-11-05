package com.cuymonitor.backend.integration;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

// Base of the integration tests: the whole application against a throwaway Postgres 18 (Testcontainers) with
// the real migrations of cuy-monitor-db, applied by Flyway when the context starts. One container for every
// test class (it starts on first use and dies with the JVM). Without Docker or without the migrations the
// tests are skipped, never failed: the unit tests still run on any laptop.
@SpringBootTest(properties = {
        "app.auth.jwt.secret=integration-secret-with-at-least-32-bytes!!",
        "app.api-key=integration-key"
})
@EnabledIf("com.cuymonitor.backend.integration.PostgresIntegrationTest#canRun")
public abstract class PostgresIntegrationTest {

    protected static final String API_KEY = "integration-key";

    private static PostgreSQLContainer postgres;

    // where the schema lives: CUY_MONITOR_DB_MIGRATIONS, or the cuy-monitor-db repo next to (CI) or beside
    // (a laptop) this one
    static Optional<Path> migrations() {
        return Stream.of(System.getenv("CUY_MONITOR_DB_MIGRATIONS"), "cuy-monitor-db/migrations",
                        "../cuy-monitor-db/migrations")
                .filter(path -> path != null && !path.isBlank())
                .map(Path::of)
                .filter(Files::isDirectory)
                .findFirst();
    }

    static boolean canRun() {
        return migrations().isPresent() && DockerClientFactory.instance().isDockerAvailable();
    }

    private static synchronized PostgreSQLContainer postgres() {
        if (postgres == null) {
            postgres = new PostgreSQLContainer("postgres:18")
                    .withDatabaseName("cuymonitor")
                    .withUsername("cuymonitor")
                    .withPassword("cuymonitor");
            postgres.start();
        }
        return postgres;
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        var container = postgres();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        // the backend never migrates in production (ADR-008); here Flyway applies cuy-monitor-db's migrations
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations",
                () -> "filesystem:" + migrations().orElseThrow().toAbsolutePath());
    }
}
