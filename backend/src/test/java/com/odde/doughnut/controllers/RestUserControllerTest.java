package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestUserControllerTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  RestUserController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestUserController(makeMe.modelFactoryService, userModel);
  }

  @Test
  void createUserWhileSessionTimeout() {
    assertThrows(
        ResponseStatusException.class, () -> controller.createUser(null, userModel.getEntity()));
  }

  @Test
  void updateUserSuccessfully() throws UnexpectedNoAccessRightException {
    User response = controller.updateUser(userModel.getEntity());
    assertThat(response, equalTo(userModel.getEntity()));
  }

  @Test
  void updateOtherUserProfile() {
    User anotherUser = makeMe.aUser().please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.updateUser(anotherUser));
  }
}
