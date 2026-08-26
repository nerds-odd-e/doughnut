package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.GeneratedTokenDTO;
import com.odde.donut.controllers.dto.TokenConfigDTO;
import com.odde.donut.entities.User;
import com.odde.donut.entities.UserToken;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class UserTokenControllerTest extends ControllerTestBase {
  @Autowired UserController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void generateTokenReturnsLabelAndToken() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("TEST_LABEL");
    GeneratedTokenDTO generated = controller.generateToken(tokenConfig);

    assertThat(generated.label(), equalTo("TEST_LABEL"));
    assertThat(generated.token().length(), equalTo(36));
  }

  @Test
  void getTokenInfoReturnsTokenLabel() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("My Token");
    GeneratedTokenDTO generated = controller.generateToken(tokenConfig);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + generated.token());

    assertThat(controller.getTokenInfo(request).getLabel(), equalTo("My Token"));
  }

  @Test
  void getTokenInfoAcceptsTestAccessTokenForExistingUser() {
    User user = makeMe.aUser().please();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token-of-" + user.getExternalIdentifier());
    UserToken tokenInfo = controller.getTokenInfo(request);

    assertThat(tokenInfo.getId(), equalTo(0));
    assertThat(tokenInfo.getUserId(), equalTo(user.getId()));
    assertThat(
        tokenInfo.getLabel(), equalTo("Test access token (" + user.getExternalIdentifier() + ")"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"Bearer invalid-token"})
  void getTokenInfoReturns401ForMissingOrInvalidAuth(String authorization) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (authorization != null && !authorization.isEmpty()) {
      request.addHeader("Authorization", authorization);
    }
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.getTokenInfo(request));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void getTokensReturnsOwnTokens() {
    makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("TEST_LABEL").please();

    List<UserToken> tokens = controller.getTokens();

    assertThat(tokens, hasSize(1));
    assertThat(tokens.getFirst().getLabel(), equalTo("TEST_LABEL"));
  }

  @Test
  void deleteTokenRemovesOwnToken() {
    UserToken userToken =
        makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("DELETE_LABEL").please();

    controller.deleteToken(userToken.getId());

    assertThat(controller.getTokens(), hasSize(0));
  }

  @Test
  void revokeTokenDeletesTokenByBearerAuth() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("Revokable Token");
    GeneratedTokenDTO generated = controller.generateToken(tokenConfig);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + generated.token());
    controller.revokeToken(request);

    assertThat(controller.getTokens(), hasSize(0));
  }

  @Test
  void revokeTokenReturns401ForInvalidToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.revokeToken(request));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void deleteTokenDeniedForAnotherUser() {
    UserToken otherToken =
        makeMe.aUserToken().forUser(makeMe.aUser().please()).withLabel("OTHER_USER_TOKEN").please();

    assertThrows(ResponseStatusException.class, () -> controller.deleteToken(otherToken.getId()));
  }
}
