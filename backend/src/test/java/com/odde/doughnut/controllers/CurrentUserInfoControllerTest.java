package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.controllers.dto.CurrentUserInfo;
import com.odde.doughnut.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentUserInfoControllerTest {
  @Mock CurrentUserFetcher currentUserFetcher;

  CurrentUserInfoController controller(CurrentUserFetcher currentUserFetcher) {
    return new CurrentUserInfoController(currentUserFetcher);
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForLearner() {
    User user = userWith("learner-ext", "Learner");
    CurrentUserInfo currentUserInfo = currentUserInfoFor(user);

    assertThat(currentUserInfo.externalIdentifier, equalTo("learner-ext"));
    assertThat(currentUserInfo.user, equalTo(user));
    assertFalse(currentUserInfo.user.isAdmin());
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForAdmin() {
    User user = userWith("admin", "Admin");
    CurrentUserInfo currentUserInfo = currentUserInfoFor(user);

    assertThat(currentUserInfo.externalIdentifier, equalTo("admin"));
    assertThat(currentUserInfo.user, equalTo(user));
    assertTrue(currentUserInfo.user.isAdmin());
  }

  private static User userWith(String externalId, String name) {
    User user = new User();
    user.setExternalIdentifier(externalId);
    user.setName(name);
    return user;
  }

  private CurrentUserInfo currentUserInfoFor(User user) {
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(user.getExternalIdentifier());
    when(currentUserFetcher.getUser()).thenReturn(user);
    return controller(currentUserFetcher).currentUserInfo();
  }
}
