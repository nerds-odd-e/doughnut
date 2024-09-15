package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record Authorization(User user, ModelFactoryService modelFactoryService) {

  public <T> void assertAuthorization(T object) throws UnexpectedNoAccessRightException {
    switch (object) {
      case Note obj -> assertAuthorizationNote(obj);
      case Notebook obj -> assertAuthorizationNotebook(obj);
      case BazaarNotebook obj -> assertAuthorizationBazaarNotebook(obj);
      case Circle obj -> assertAuthorizationCircle(obj);
      case Subscription obj -> assertAuthorizationSubscription(obj);
      case User obj -> assertAuthorizationUser(obj);
      case AssessmentAttempt obj -> assertAuthorizationUser(obj.getUser());
      default ->
          throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  private void assertAuthorizationBazaarNotebook(BazaarNotebook object)
      throws UnexpectedNoAccessRightException {
    if (!isAdmin()) {
      assertAuthorizationNotebook(object.getNotebook());
    }
  }

  public <T> void assertReadAuthorization(T object) throws UnexpectedNoAccessRightException {
    switch (object) {
      case Note obj -> assertReadAuthorizationNote(obj);
      case Notebook obj -> assertReadAuthorizationNotebook(obj);
      case Subscription obj -> assertReadAuthorization(obj);
      case Answer obj -> assertReadAuthorizationAnswer(obj);
      case PredefinedQuestion obj -> assertReadAuthorizationPredefinedQuestion(obj);
      case ReviewPoint obj -> assertReadAuthorizationReviewPoint(obj);
      case User obj -> assertAuthorizationUser(obj);
      case Audio obj -> assertAuthorizationUser(obj.getUser());
      default ->
          throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  private void assertReadAuthorizationReviewPoint(ReviewPoint object)
      throws UnexpectedNoAccessRightException {
    assertAuthorizationUser(object.getUser());
  }

  private void assertReadAuthorizationAnswer(Answer object)
      throws UnexpectedNoAccessRightException {
    assertReadAuthorizationPredefinedQuestion(
        object.getReviewQuestionInstance().getPredefinedQuestion());
  }

  private void assertReadAuthorizationPredefinedQuestion(PredefinedQuestion question)
      throws UnexpectedNoAccessRightException {
    assertReadAuthorization(question.getNote());
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
}
