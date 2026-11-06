package com.cuymonitor.backend;

import com.cuymonitor.backend.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;

// the whole context against the real schema (Testcontainers): Hibernate validates every entity against it
class BackendApplicationTests extends PostgresIntegrationTest {

	@Test
	void contextLoads() {
	}

}
