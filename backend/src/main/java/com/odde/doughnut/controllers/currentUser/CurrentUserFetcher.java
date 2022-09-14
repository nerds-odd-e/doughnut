package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.models.UserModel;

public interface CurrentUserFetcher {
  UserModel getUser();

  String getExternalIdentifier();
}
