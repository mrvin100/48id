package io.k48.fortyeightid;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModularityTests {

	@Test
	void verifyModularStructure() {
		ApplicationModules.of(Application.class).verify();
	}
}
