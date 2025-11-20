package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.TokenConfigDTO;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
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
  void createUserWhileSessionTimeout() {
    assertThrows(
        ResponseStatusException.class, () -> controller.createUser(null, currentUser.getUser()));
  }

  @Test
  void updateUserSuccessfully() throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName("new name");
    dto.setSpaceIntervals("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15");
    dto.setDailyAssimilationCount(12);
    User response = controller.updateUser(currentUser.getUser(), dto);
    assertThat(response.getName(), equalTo(dto.getName()));
    assertThat(response.getSpaceIntervals(), equalTo(dto.getSpaceIntervals()));
    assertThat(response.getDailyAssimilationCount(), equalTo(dto.getDailyAssimilationCount()));
  }

  @Test
  void updateOtherUserProfile() {
    UserDTO dto = new UserDTO();
    dto.setName("new name");
    User anotherUser = makeMe.aUser().please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.updateUser(anotherUser, dto));
  }

  @Test
  void generateTokenShouldReturnValidUserToken() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("TEST_LABEL");
    UserToken userToken = controller.generateToken(tokenConfig);

    assertThat(userToken.getUserId(), equalTo(currentUser.getUser().getId()));
    assertThat(userToken.getLabel(), equalTo("TEST_LABEL"));
    assertThat(userToken.getToken().length(), equalTo(36));
  }

  @Test
  void getTokensTest() {
    UserToken userToken =
        makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("TEST_LABEL").please();
    makeMe.entityPersister.save(userToken);

    List<UserToken> getTokens = controller.getTokens();

    assertTrue(getTokens.stream().anyMatch(el -> el.getLabel().equals("TEST_LABEL")));
    assertThat(getTokens.size(), equalTo(1));
  }

  @Test
  void getTokensWithMultipleTokens() {
    UserToken userToken = new UserToken(currentUser.getUser().getId(), "token", "LABEL");
    makeMe.entityPersister.save(userToken);

    List<UserToken> getTokens = controller.getTokens();

    assertTrue(getTokens.stream().anyMatch(el -> el.getLabel().equals("LABEL")));
    assertThat(getTokens.size(), equalTo(1));
  }

  @Test
  void deleteTokenTest() {
    UserToken userToken =
        makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("DELETE_LABEL").please();
    makeMe.entityPersister.save(userToken);

    controller.deleteToken(userToken.getId());

    List<UserToken> getTokens = controller.getTokens();
    assertFalse(getTokens.stream().anyMatch(el -> el.getId().equals(userToken.getId())));
    assertThat(getTokens.size(), equalTo(0));
  }

  @Test
  void deleteTokenTestForAnotherUser() {
    User anotherUser = makeMe.aUser().please();
    UserToken userToken2 =
        makeMe.aUserToken().forUser(anotherUser).withLabel("OTHER_USER_TOKEN").please();
    makeMe.entityPersister.save(userToken2);

    assertThrows(ResponseStatusException.class, () -> controller.deleteToken(userToken2.getId()));
  }
}
