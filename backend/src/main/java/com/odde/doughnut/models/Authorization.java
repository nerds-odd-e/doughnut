package com.odde.doughnut.models;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record Authorization(User user, ModelFactoryService modelFactoryService) {

  public <T> void assertAuthorization(T object) throws NoAccessRightException {
    if (object instanceof Note) {
      assertAuthorization((Note) object);
    } else if (object instanceof Notebook) {
      assertAuthorization((Notebook) object);
    } else if (object instanceof Circle) {
      assertAuthorization((Circle) object);
    } else if (object instanceof Subscription) {
      assertAuthorization((Subscription) object);
    } else if (object instanceof User) {
      assertAuthorization((User) object);
    } else if (object instanceof Link) {
      assertAuthorization((Link) object);
    } else {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  private void assertAuthorization(Note note) throws NoAccessRightException {
    assertLoggedIn();
    if (!hasFullAuthority(note.getNotebook())) {
      throw new NoAccessRightException();
    }
  }

  public void assertReadAuthorization(Note note) throws NoAccessRightException {
    assertReadAuthorization(note.getNotebook());
  }

  private boolean hasFullAuthority(Notebook notebook) {
    if (user == null) return false;
    return user.owns(notebook);
  }

  private boolean hasReferenceAuthority(Note note) {
    if (user == null) return false;
    return user.canReferTo(note.getNotebook());
  }

  public void assertReadAuthorization(Notebook notebook) throws NoAccessRightException {
    if (notebook != null) {
      if (hasReferenceAuthority(notebook.getHeadNote())) {
        return;
      }
      if (modelFactoryService.bazaarNotebookRepository.findByNotebook(notebook) != null) {
        return;
      }
    }
    assertLoggedIn();
    throw new NoAccessRightException();
  }

  private void assertAuthorization(Notebook notebook) throws NoAccessRightException {
    if (!hasFullAuthority(notebook)) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorization(Circle circle) throws NoAccessRightException {
    assertLoggedIn();
    if (user == null || !user.inCircle(circle)) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorization(Subscription subscription) throws NoAccessRightException {
    if (subscription.getUser() != user) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorization(User user) throws NoAccessRightException {
    if (!this.user.getId().equals(user.getId())) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorization(Link link) throws NoAccessRightException {
    if (!link.getSourceNote().getThing().getCreator().getId().equals(user.getId())) {
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

  public void assertLoggedIn() {
    if (user == null) {
      throwUserNotFound();
    }
  }

  public static void throwUserNotFound() {
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User Not Found");
  }

  public void assertReadAuthorization(Link link) throws NoAccessRightException {
    assertReadAuthorization(link.getSourceNote());
  }
}
