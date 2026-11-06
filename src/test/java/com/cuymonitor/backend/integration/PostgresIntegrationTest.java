package com.cuymonitor.backend.integration;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Base of the integration tests: the whole application against the throwaway Postgres of TestPostgres. Without
// Docker or without the migrations the tests are skipped, never failed.
@SpringBootTest(properties = {
        "app.auth.jwt.secret=integration-secret-with-at-least-32-bytes!!",
        "app.api-key=integration-key"
})
@EnabledIf("com.cuymonitor.backend.integration.TestPostgres#canRun")
public abstract class PostgresIntegrationTest {

    protected static final String API_KEY = "integration-key";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestPostgres.register(registry);
    }
}
