package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;

public class CurrentUser {
  private final UserModel userModel;

  public CurrentUser(UserModel userModel) {
    this.userModel = userModel;
  }

  public UserModel getUserModel() {
    return userModel;
  }

  public User getUser() {
    return userModel.getEntity();
  }
}
