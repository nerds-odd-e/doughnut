package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.controllers.dto.CurrentUserInfo;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CurrentUserInfoControllerTest {
  @Autowired MakeMe makeMe;

  @Mock CurrentUserFetcher currentUserFetcher;

  CurrentUserInfoController controller(CurrentUserFetcher currentUserFetcher) {
    return new CurrentUserInfoController(currentUserFetcher);
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForLearner() {
    User user = makeMe.aUser().please();
    String externalId = user.getExternalIdentifier();
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
    when(currentUserFetcher.getUser()).thenReturn(user);
    CurrentUserInfo currentUserInfo = controller(currentUserFetcher).currentUserInfo();

    assertThat(currentUserInfo.externalIdentifier, equalTo(externalId));
    assertThat(currentUserInfo.user, equalTo(user));
    assertFalse(currentUserInfo.user.isAdmin());
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForAdmin() {
    User user = makeMe.anAdmin().please();
    String externalId = user.getExternalIdentifier();
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
    when(currentUserFetcher.getUser()).thenReturn(user);
    CurrentUserInfo currentUserInfo = controller(currentUserFetcher).currentUserInfo();

    assertThat(currentUserInfo.externalIdentifier, equalTo(externalId));
    assertThat(currentUserInfo.user, equalTo(user));
    assertTrue(currentUserInfo.user.isAdmin());
  }
}
