package com.odde.doughnut.examples;

import com.odde.doughnut.controllers.IndexController;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DoughnutApplicationTests {
	@Autowired IndexController controller;
	@Mock Model model;

	@Test
	void visitWithNoUserSession() {
		assertEquals("vuejsed", controller.home(null, model));
	}

}
