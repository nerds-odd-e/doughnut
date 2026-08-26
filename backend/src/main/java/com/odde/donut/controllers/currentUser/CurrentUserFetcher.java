package com.odde.donut.controllers.currentUser;

import com.odde.donut.entities.User;

public interface CurrentUserFetcher {
  User getUser();

  String getExternalIdentifier();
}
