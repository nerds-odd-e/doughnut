package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class IndexControllerTests {

  @Autowired private ModelFactoryService modelFactoryService;
  @Mock Model model;
  IndexController controller;
  @Autowired MakeMe makeMe;

  @Test
  void visitWithNoUserSession() {
    controller = new IndexController(new TestCurrentUserFetcher(modelFactoryService.toUserModel(null)), modelFactoryService);
    assertEquals("vuejsed", controller.home(null, model));
  }

  @Test
  void visitWithUserSessionButNoSuchARegisteredUserYet() {
    controller = new IndexController(new TestCurrentUserFetcher(modelFactoryService.toUserModel(null)), modelFactoryService);
    Principal principal = (UserPrincipal) () -> "1234567";
    assertEquals("vuejsed", controller.home(principal, model));
  }

  @Test
  void visitWithUserSessionAndTheUserExists() {
    User user = makeMe.aUser().please();
    Principal principal = (UserPrincipal) user::getExternalIdentifier;
    controller = new IndexController(new TestCurrentUserFetcher(modelFactoryService.toUserModel(user)), modelFactoryService);
    assertEquals("vuejsed", controller.home(principal, model));
  }
}
