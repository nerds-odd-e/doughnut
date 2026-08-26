package com.odde.donut.controllers.currentUser;

import com.odde.donut.entities.User;

public class CurrentUser {
  private User user;

  public CurrentUser(User user) {
    this.user = user;
  }

  public CurrentUser() {
    this.user = null;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
