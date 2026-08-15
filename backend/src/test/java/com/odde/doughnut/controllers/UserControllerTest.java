package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class UserControllerTest extends ControllerTestBase {
  @Autowired UserController controller;
  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void createUserRequiresSession() {
    assertThrows(
        ResponseStatusException.class, () -> controller.createUser(null, currentUser.getUser()));
  }

  @Test
  void updateUserPersistsProfileFields() throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName("new name");
    dto.setDailyAssimilationCount(12);
    User response = controller.updateUser(currentUser.getUser(), dto);
    assertThat(response.getName(), equalTo(dto.getName()));
    assertThat(response.getDailyAssimilationCount(), equalTo(dto.getDailyAssimilationCount()));
  }

  @Test
  void updateUserDoesNotChangeStoredSpaceIntervals() throws UnexpectedNoAccessRightException {
    User user = currentUser.getUser();
    user.setSpaceIntervals("9, 9, 9");
    UserDTO dto = new UserDTO();
    dto.setName(user.getName());
    dto.setDailyAssimilationCount(user.getDailyAssimilationCount());
    assertThat(controller.updateUser(user, dto).getSpaceIntervals(), equalTo("9, 9, 9"));
  }

  @Test
  void userProfileJsonOmitsSpaceIntervals() throws Exception {
    JsonNode json =
        objectMapper.readTree(objectMapper.writeValueAsString(controller.getUserProfile()));
    assertThat(json.has("spaceIntervals"), is(false));
  }

  @Test
  void updateOtherUserProfileDenied() {
    UserDTO dto = new UserDTO();
    dto.setName("new name");
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updateUser(makeMe.aUser().please(), dto));
  }

  @Test
  void healthRemoveEmptyFoldersDefaultIsFalse() {
    assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(false));
  }

  @Test
  void updateUserPersistsHealthRemoveEmptyFoldersDefault() throws UnexpectedNoAccessRightException {
    User user = currentUser.getUser();
    UserDTO dto = new UserDTO();
    dto.setName(user.getName());
    dto.setDailyAssimilationCount(user.getDailyAssimilationCount());
    dto.setHealthRemoveEmptyFoldersDefault(true);

    assertThat(
        controller.updateUser(user, dto).getHealthRemoveEmptyFoldersDefault(), equalTo(true));
    assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(true));
  }
}
