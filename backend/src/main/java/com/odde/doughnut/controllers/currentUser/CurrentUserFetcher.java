package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;

public interface CurrentUserFetcher {
  UserModel getUser();

  String getExternalIdentifier();

  void setExternalIdentifier(String name);

  default void assertAuthorization(Note note) throws NoAccessRightException {
    getUser().getAuthorization().assertAuthorization(note);
  }

  default void assertReadAuthorization(Note note) throws NoAccessRightException {
    getUser().getAuthorization().assertReadAuthorization(note);
  }
}
