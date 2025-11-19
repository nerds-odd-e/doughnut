package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.User;

public interface CurrentUserFetcher {
  User getUser();

  String getExternalIdentifier();
}
