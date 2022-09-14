package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.models.UserModel;

public class TestCurrentUserFetcher implements CurrentUserFetcher {
  private final UserModel userModel;

  public TestCurrentUserFetcher(UserModel userModel) {
    this.userModel = userModel;
  }

  @Override
  public UserModel getUser() {
    return userModel;
  }

  @Override
  public String getExternalIdentifier() {
    if (!userModel.loggedIn()) {
      return null;
    }
    return userModel.getEntity().getExternalIdentifier();
  }
}
