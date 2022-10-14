package com.odde.doughnut.models;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record Authorization(User user, ModelFactoryService modelFactoryService) {

  public <T> void assertAuthorization(T object) throws UnexpectedNoAccessRightException {
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

  public <T> void assertReadAuthorization(T object) throws UnexpectedNoAccessRightException {
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

  private void assertAuthorizationNote(Note note) throws UnexpectedNoAccessRightException {
    assertLoggedIn();
    if (!hasFullAuthority(note.getNotebook())) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertReadAuthorizationNote(Note note) throws UnexpectedNoAccessRightException {
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

  private void assertReadAuthorizationNotebook(Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (notebook != null) {
      if (hasReferenceAuthority(notebook.getHeadNote())) {
        return;
      }
      if (modelFactoryService.bazaarNotebookRepository.findByNotebook(notebook) != null) {
        return;
      }
    }
    assertLoggedIn();
    throw new UnexpectedNoAccessRightException();
  }

  private void assertAuthorizationNotebook(Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (!hasFullAuthority(notebook)) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationCircle(Circle circle) throws UnexpectedNoAccessRightException {
    assertLoggedIn();
    if (user == null || !user.inCircle(circle)) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationSubscription(Subscription subscription)
      throws UnexpectedNoAccessRightException {
    if (subscription.getUser() != user) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationUser(User user) throws UnexpectedNoAccessRightException {
    if (!this.user.getId().equals(user.getId())) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationLink(Link link) throws UnexpectedNoAccessRightException {
    if (!link.getSourceNote().getThing().getCreator().getId().equals(user.getId())) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  public void assertDeveloperAuthorization() throws UnexpectedNoAccessRightException {
    if (!isDeveloper()) {
      throw new UnexpectedNoAccessRightException();
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

  private void assertReadAuthorizationLink(Link link) throws UnexpectedNoAccessRightException {
    assertReadAuthorizationNote(link.getSourceNote());
  }
}
