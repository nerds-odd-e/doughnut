package com.odde.doughnut.examples;

import com.odde.doughnut.controllers.IndexController;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.Model;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
class DoughnutApplicationTests {
	@Autowired IndexController controller;
	@Mock Model model;

	@Test
	void visitWithNoUserSession() {
		assertEquals("login", controller.home(null, model));
	}

}
