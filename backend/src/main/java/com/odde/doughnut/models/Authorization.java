package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.Arrays;
import java.util.List;

public class Authorization {
  protected final User user;
  protected final ModelFactoryService modelFactoryService;

  public Authorization(User user, ModelFactoryService modelFactoryService) {
    this.user = user;
    this.modelFactoryService = modelFactoryService;
  }

  public void assertAuthorization(Note note) throws NoAccessRightException {
    if (!hasFullAuthority(note)) {
      throw new NoAccessRightException();
    }
  }

  public boolean hasFullAuthority(Note note) {
    return hasFullAuthority(note.getNotebook());
  }

  public boolean hasFullAuthority(Notebook notebook) {
    return user.owns(notebook);
  }

  public boolean hasReferenceAuthority(Note note) {
    if (user == null) return false;
    return user.canReferTo(note.getNotebook());
  }

  public void assertReadAuthorization(Note note) throws NoAccessRightException {
    if (!hasReferenceAuthority(note)) {
      throw new NoAccessRightException();
    }
  }

  public void assertAuthorization(Notebook notebook)
      throws NoAccessRightException {
    if (!hasFullAuthority(notebook)) {
      throw new NoAccessRightException();
    }
  }

  public void assertAuthorization(Circle circle) throws NoAccessRightException {
    if (!user.inCircle(circle)) {
      throw new NoAccessRightException();
    }
  }

  public void assertAuthorization(Subscription subscription)
      throws NoAccessRightException {
    if (subscription.getUser() != user) {
      throw new NoAccessRightException();
    }
  }

  public void assertAuthorization(User user) throws NoAccessRightException {
    if (!this.user.getId().equals(user.getId())) {
      throw new NoAccessRightException();
    }
  }

  public void assertAuthorization(Link link) throws NoAccessRightException {
    if (link.getUser().getId() != user.getId()) {
      throw new NoAccessRightException();
    }
  }

  public void assertPotentialReadAuthorization(Notebook notebook)
      throws NoAccessRightException {
    if (!hasReferenceAuthority(notebook.getHeadNote()) &&
        modelFactoryService.bazaarNotebookRepository.findByNotebook(notebook) ==
            null) {
      throw new NoAccessRightException();
    }
  }

  public void assertDeveloperAuthorization() throws NoAccessRightException {
    if (!isDeveloper()) {
      throw new NoAccessRightException();
    }
  }

  private static final List<String> allowUsers =
      Arrays.asList("Terry", "t-machu", "Developer", "Yeong Sheng");

  public boolean isDeveloper() {
    if (user == null) return false;
    return allowUsers.contains(user.getName());
  }
}
