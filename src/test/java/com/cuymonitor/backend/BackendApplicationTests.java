package com.cuymonitor.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
