package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.*;
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

  default void assertAuthorization(Notebook notebook) throws NoAccessRightException {
    getUser().getAuthorization().assertAuthorization(notebook);
  }

  default void assertReadAuthorization(Notebook notebook) throws NoAccessRightException {
    getUser().getAuthorization().assertReadAuthorization(notebook);
  }

  default void assertAuthorization(Circle circle) throws NoAccessRightException {
    getUser().getAuthorization().assertAuthorization(circle);
  }

  default void assertAuthorization(Subscription subscription) throws NoAccessRightException {
    getUser().getAuthorization().assertAuthorization(subscription);
  }

  default void assertAuthorization(User user) throws NoAccessRightException {
    getUser().getAuthorization().assertAuthorization(user);
  }

  default void assertAuthorization(Link link) throws NoAccessRightException {
    getUser().getAuthorization().assertAuthorization(link);
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
