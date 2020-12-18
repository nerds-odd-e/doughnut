package com.odde.doughnut;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles(profiles = "test")
class DoughnutApplicationTests {
	@Autowired
    IndexController controller;

	@Test
	void contextLoads() {
		Model model = mock(Model.class);

		assertEquals("login", controller.home(null, model));
	}

}
