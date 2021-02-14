package com.odde.doughnut.examples;

import com.odde.doughnut.controllers.IndexController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.Model;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
class DoughnutApplicationTests {
	@Autowired
	IndexController controller;

	@Test
	void visitWithNoUserSession() {
		Model model = mock(Model.class);
		assertEquals("login", controller.home(null, model));
	}

	@Test
	void visitWithUserSessionButNoSuchARegisteredUserYet() {
		Principal user = new UserPrincipal() {
			@Override
			public String getName() {
				return "Tom";
			}
		};
		Model model = mock(Model.class);
		assertEquals("index", controller.home(user, model));
	}

}
