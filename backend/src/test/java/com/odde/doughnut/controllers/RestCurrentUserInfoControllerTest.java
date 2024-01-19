package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.controllers.json.CurrentUserInfo;
import com.odde.doughnut.models.UserModel;
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
class RestCurrentUserInfoControllerTest {
  @Autowired MakeMe makeMe;

  @Mock CurrentUserFetcher currentUserFetcher;

  RestCurrentUserInfoController controller(CurrentUserFetcher currentUserFetcher) {
    return new RestCurrentUserInfoController(currentUserFetcher);
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForLearner() {
    UserModel userModel = makeMe.aUser().toModelPlease();
    String externalId = userModel.getEntity().getExternalIdentifier();
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
    when(currentUserFetcher.getUser()).thenReturn(userModel);
    CurrentUserInfo currentUserInfo = controller(currentUserFetcher).currentUserInfo();

    assertThat(currentUserInfo.externalIdentifier, equalTo(externalId));
    assertThat(currentUserInfo.user, equalTo(userModel.getEntity()));
    assertFalse(currentUserInfo.user.isAdmin());
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForAdmin() {
    UserModel userModel = makeMe.anAdmin().toModelPlease();
    String externalId = userModel.getEntity().getExternalIdentifier();
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
    when(currentUserFetcher.getUser()).thenReturn(userModel);
    CurrentUserInfo currentUserInfo = controller(currentUserFetcher).currentUserInfo();

    assertThat(currentUserInfo.externalIdentifier, equalTo(externalId));
    assertThat(currentUserInfo.user, equalTo(userModel.getEntity()));
    assertTrue(currentUserInfo.user.isAdmin());
  }
}
