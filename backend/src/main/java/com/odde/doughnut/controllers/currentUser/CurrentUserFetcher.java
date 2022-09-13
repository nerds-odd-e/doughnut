package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;

public interface CurrentUserFetcher {
  UserModel getUser();

  String getExternalIdentifier();

  void setExternalIdentifier(String name);

  default User getUserEntity() {
    return getUser().getEntity();
  }

  default <T> void assertAuthorization(T object) throws NoAccessRightException {
    getUser().getAuthorization().assertAuthorization(object);
  }

  default void assertReadAuthorization(Note note) throws NoAccessRightException {
    getUser().getAuthorization().assertReadAuthorization(note);
  }

  default void assertReadAuthorization(Notebook notebook) throws NoAccessRightException {
    getUser().getAuthorization().assertReadAuthorization(notebook);
  }

  default void assertReadAuthorization(Link link) throws NoAccessRightException {
    getUser().getAuthorization().assertReadAuthorization(link);
  }

  default void assertDeveloperAuthorization() throws NoAccessRightException {
    getUser().getAuthorization().assertDeveloperAuthorization();
  }

  default void assertLoggedIn() {
    getUser().getAuthorization().assertLoggedIn();
  }
}
