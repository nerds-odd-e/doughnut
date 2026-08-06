package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.controllers.dto.CurrentUserInfo;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestAccessTokenResolver;
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
