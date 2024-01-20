package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
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
    } else if (object instanceof Answer) {
      assertReadAuthorizationAnswer((Answer) object);
    } else if (object instanceof QuizQuestionEntity) {
      assertReadAuthorizationQuizQuestion((QuizQuestionEntity) object);
    } else if (object instanceof ReviewPoint) {
      assertReadAuthorizationReviewPoint((ReviewPoint) object);
    } else if (object instanceof Thing) {
      assertReadAuthorizationThing((Thing) object);
    } else if (object instanceof User) {
      assertAuthorizationUser((User) object);
    } else {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  private void assertReadAuthorizationThing(Thing object) throws UnexpectedNoAccessRightException {
    if (object.getNote() != null) {
      assertReadAuthorizationNote(object.getNote());
    } else {
      assertReadAuthorizationLink(object.getLink());
    }
  }

  private void assertReadAuthorizationReviewPoint(ReviewPoint object)
      throws UnexpectedNoAccessRightException {
    assertAuthorizationUser(object.getUser());
  }

  private void assertReadAuthorizationAnswer(Answer object)
      throws UnexpectedNoAccessRightException {
    assertReadAuthorizationQuizQuestion(object.getQuestion());
  }

  private void assertReadAuthorizationQuizQuestion(QuizQuestionEntity question)
      throws UnexpectedNoAccessRightException {
    assertReadAuthorizationThing(question.getThing());
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

  private void assertReadAuthorizationNotebook(Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (notebook != null) {
      if (user != null && user.canReferTo(notebook)) {
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
      System.out.printf("user: %s, circle: %s%n", user, circle);
      System.out.printf("user: %s, circle: %s%n", user, circle);
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

  public void assertAdminAuthorization() throws UnexpectedNoAccessRightException {
    if (!isAdmin()) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  public boolean isAdmin() {
    return user != null && user.isAdmin();
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
