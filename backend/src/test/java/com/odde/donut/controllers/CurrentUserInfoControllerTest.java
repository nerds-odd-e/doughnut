package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.donut.controllers.dto.CurrentUserInfo;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.UserRepository;
import com.odde.donut.services.UserService;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.testability.TestAccessTokenResolver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CurrentUserInfoControllerTest {
  @Autowired MakeMe makeMe;
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Autowired TestAccessTokenResolver testAccessTokenResolver;

  @Test
  void returnsExternalIdentifierAndUser() {
    User user = makeMe.aUser().please();
    CurrentUserInfo info = currentUserInfoFor(user);

    assertThat(info.externalIdentifier, equalTo(user.getExternalIdentifier()));
    assertThat(info.user.getId(), equalTo(user.getId()));
    assertFalse(info.user.isAdmin());
  }

  @Test
  void adminUserIsFlagged() {
    User admin = makeMe.anAdmin().please();
    assertTrue(currentUserInfoFor(admin).user.isAdmin());
  }

  private CurrentUserInfo currentUserInfoFor(User user) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token-of-" + user.getExternalIdentifier());
    CurrentUserFetcherFromRequest fetcher =
        new CurrentUserFetcherFromRequest(
            request, userRepository, userService, Optional.of(testAccessTokenResolver));
    return new CurrentUserInfoController(fetcher).currentUserInfo();
  }
}
