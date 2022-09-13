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
      assertAuthorizationNote((Note) object);
    } else if (object instanceof Notebook) {
      assertAuthorizationNotebook((Notebook) object);
    } else if (object instanceof Circle) {
      assertAuthorizationCircle((Circle) object);
    } else if (object instanceof Subscription) {
      assertAuthorizationSubscription((Subscription) object);
    } else if (object instanceof User) {
      assertAuthorizationUser((User) object);
    } else if (object instanceof Link) {
      assertAuthorizationLink((Link) object);
    } else {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  public <T> void assertReadAuthorization(T object) throws NoAccessRightException {
    if (object instanceof Note) {
      assertReadAuthorizationNote((Note) object);
    } else if (object instanceof Notebook) {
      assertReadAuthorizationNotebook((Notebook) object);
    } else if (object instanceof Subscription) {
      assertReadAuthorization((Subscription) object);
    } else if (object instanceof Link) {
      assertReadAuthorizationLink((Link) object);
    } else {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  private void assertAuthorizationNote(Note note) throws NoAccessRightException {
    assertLoggedIn();
    if (!hasFullAuthority(note.getNotebook())) {
      throw new NoAccessRightException();
    }
  }

  private void assertReadAuthorizationNote(Note note) throws NoAccessRightException {
    assertReadAuthorizationNotebook(note.getNotebook());
  }

  private boolean hasFullAuthority(Notebook notebook) {
    if (user == null) return false;
    return user.owns(notebook);
  }

  private boolean hasReferenceAuthority(Note note) {
    if (user == null) return false;
    return user.canReferTo(note.getNotebook());
  }

  private void assertReadAuthorizationNotebook(Notebook notebook) throws NoAccessRightException {
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

  private void assertAuthorizationNotebook(Notebook notebook) throws NoAccessRightException {
    if (!hasFullAuthority(notebook)) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorizationCircle(Circle circle) throws NoAccessRightException {
    assertLoggedIn();
    if (user == null || !user.inCircle(circle)) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorizationSubscription(Subscription subscription)
      throws NoAccessRightException {
    if (subscription.getUser() != user) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorizationUser(User user) throws NoAccessRightException {
    if (!this.user.getId().equals(user.getId())) {
      throw new NoAccessRightException();
    }
  }

  private void assertAuthorizationLink(Link link) throws NoAccessRightException {
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

  private void assertReadAuthorizationLink(Link link) throws NoAccessRightException {
    assertReadAuthorizationNote(link.getSourceNote());
  }
}
