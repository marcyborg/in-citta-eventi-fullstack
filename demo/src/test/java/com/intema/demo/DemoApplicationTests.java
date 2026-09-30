package com.intema.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"h2", "test"})
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
