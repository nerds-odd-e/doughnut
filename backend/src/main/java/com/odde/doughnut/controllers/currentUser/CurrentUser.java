package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.User;

public class CurrentUser {
  private User user;

  public CurrentUser(User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
