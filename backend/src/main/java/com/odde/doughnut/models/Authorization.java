package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.Arrays;
import java.util.List;

public class Authorization extends ModelForEntity<User> {
  public Authorization(User entity, ModelFactoryService modelFactoryService) {
    super(entity, modelFactoryService);
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
    return entity.owns(notebook);
  }

  public boolean hasReadAuthority(Note note) {
    return this.entity.canRead(note.getNotebook());
  }

  public void assertReadAuthorization(Note note) throws NoAccessRightException {
    if (!hasReadAuthority(note)) {
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
    if (!entity.inCircle(circle)) {
      throw new NoAccessRightException();
    }
  }

  public void assertAuthorization(User user) throws NoAccessRightException {
    if (!entity.getId().equals(user.getId())) {
      throw new NoAccessRightException();
    }
  }

  public void assertAuthorization(Link link) throws NoAccessRightException {
    if (link.getUser().getId() != entity.getId()) {
      throw new NoAccessRightException();
    }
  }

  public void assertPotentialReadAuthorization(Notebook notebook)
      throws NoAccessRightException {
    if (!hasReadAuthority(notebook.getHeadNote()) &&
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
      Arrays.asList("Terry", "t-machu", "Developer", "thuzar", "Yeong Sheng");

  public boolean isDeveloper() { return allowUsers.contains(entity.getName()); }
}
