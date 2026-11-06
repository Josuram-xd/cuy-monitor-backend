package com.cuymonitor.backend.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The throwaway Postgres 18 of every test that needs a real database (Testcontainers), with the real migrations
 * of cuy-monitor-db applied by Flyway when the context starts. One container for the whole run: it starts on first
 * use and dies with the JVM. Without Docker or without the migrations those tests are skipped
 * ({@code @EnabledIf("...TestPostgres#canRun")}), never failed: the unit tests still run on any laptop.
 */
public final class TestPostgres {

    private static PostgreSQLContainer postgres;

    private TestPostgres() {
    }

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

    public static boolean canRun() {
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

    /** Points the context at the container; call it from a {@code @DynamicPropertySource} method. */
    public static void register(DynamicPropertyRegistry registry) {
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
