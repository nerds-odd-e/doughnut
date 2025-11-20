package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.services.AuthorizationService;
import org.springframework.test.util.ReflectionTestUtils;

public final class AuthorizationServiceTestHelper {
  private AuthorizationServiceTestHelper() {}

  public static void setCurrentUser(
      AuthorizationService authorizationService, CurrentUser currentUser) {
    ReflectionTestUtils.setField(authorizationService, "currentUser", currentUser);
  }
}
