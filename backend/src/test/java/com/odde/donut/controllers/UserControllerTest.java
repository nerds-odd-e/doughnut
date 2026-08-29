package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.UserDTO;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class UserControllerTest extends ControllerTestBase {
  @Autowired UserController controller;

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

  @Test
  void dailyProbeEnabledIsFalse() {
    assertThat(controller.getUserProfile().getDailyProbeEnabled(), equalTo(false));
  }

  @Test
  void updateUserPersistsDailyProbeEnabled() throws UnexpectedNoAccessRightException {
    User user = currentUser.getUser();
    UserDTO dto = new UserDTO();
    dto.setName(user.getName());
    dto.setDailyAssimilationCount(user.getDailyAssimilationCount());
    dto.setDailyProbeEnabled(true);

    assertThat(controller.updateUser(user, dto).getDailyProbeEnabled(), equalTo(true));
    assertThat(controller.getUserProfile().getDailyProbeEnabled(), equalTo(true));
  }

  @Test
  void updateUserLeavesDailyProbeEnabledWhenOmitted() throws UnexpectedNoAccessRightException {
    User user = currentUser.getUser();
    user.setDailyProbeEnabled(true);

    UserDTO healthDefaults = new UserDTO();
    healthDefaults.setName(user.getName());
    healthDefaults.setDailyAssimilationCount(user.getDailyAssimilationCount());
    healthDefaults.setHealthRemoveEmptyFoldersDefault(true);

    assertThat(controller.updateUser(user, healthDefaults).getDailyProbeEnabled(), equalTo(true));
  }
}
