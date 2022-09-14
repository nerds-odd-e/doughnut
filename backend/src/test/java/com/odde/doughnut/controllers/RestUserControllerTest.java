package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
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
  void updateUserSuccessfully() throws NoAccessRightException {
    User response = controller.updateUser(userModel.getEntity());
    assertThat(response, equalTo(userModel.getEntity()));
  }

  @Test
  void updateOtherUserProfile() {
    User anotherUser = makeMe.aUser().please();
    assertThrows(NoAccessRightException.class, () -> controller.updateUser(anotherUser));
  }
}
