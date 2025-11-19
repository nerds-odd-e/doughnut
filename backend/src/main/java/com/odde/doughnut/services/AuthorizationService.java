package com.odde.doughnut.services;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.BazaarNotebookRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationService {
  private final BazaarNotebookRepository bazaarNotebookRepository;
  private final CurrentUser currentUser;

  public AuthorizationService(
      BazaarNotebookRepository bazaarNotebookRepository, CurrentUser currentUser) {
    this.bazaarNotebookRepository = bazaarNotebookRepository;
    this.currentUser = currentUser;
  }

  public User getCurrentUser() {
    return currentUser.getUser();
  }

  public <T> void assertAuthorization(T object) throws UnexpectedNoAccessRightException {
    assertAuthorization(getCurrentUser(), object);
  }

  public <T> void assertAuthorization(User user, T object) throws UnexpectedNoAccessRightException {
    switch (object) {
      case Note obj -> assertAuthorizationNote(user, obj);
      case Notebook obj -> assertAuthorizationNotebook(user, obj);
      case BazaarNotebook obj -> assertAuthorizationBazaarNotebook(user, obj);
      case Circle obj -> assertAuthorizationCircle(user, obj);
      case Subscription obj -> assertAuthorizationSubscription(user, obj);
      case User obj -> assertAuthorizationUser(user, obj);
      case AssessmentAttempt obj -> assertAuthorizationUser(user, obj.getUser());
      case AssessmentQuestionInstance obj -> assertAuthorization(user, obj.getAssessmentAttempt());
      case Conversation obj -> assertAuthorizationConversation(user, obj);
      default ->
          throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  private void assertAuthorizationConversation(User user, Conversation obj)
      throws UnexpectedNoAccessRightException {
    if (!obj.getSubjectOwnership().ownsBy(user) && !obj.getConversationInitiator().equals(user)) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationBazaarNotebook(User user, BazaarNotebook object)
      throws UnexpectedNoAccessRightException {
    if (!isAdmin(user)) {
      assertAuthorizationNotebook(user, object.getNotebook());
    }
  }

  public <T> void assertReadAuthorization(T object) throws UnexpectedNoAccessRightException {
    assertReadAuthorization(getCurrentUser(), object);
  }

  public <T> void assertReadAuthorization(User user, T object)
      throws UnexpectedNoAccessRightException {
    switch (object) {
      case Note obj -> assertReadAuthorizationNote(user, obj);
      case Notebook obj -> assertReadAuthorizationNotebook(user, obj);
      case Subscription obj -> assertReadAuthorization(user, obj);
      case RecallPrompt obj -> assertReadAuthorizationRecallPrompt(user, obj);
      case PredefinedQuestion obj -> assertReadAuthorizationPredefinedQuestion(user, obj);
      case MemoryTracker obj -> assertReadAuthorizationMemoryTracker(user, obj);
      case User obj -> assertAuthorizationUser(user, obj);
      default ->
          throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR, "Unknown object type");
    }
  }

  private void assertReadAuthorizationMemoryTracker(User user, MemoryTracker object)
      throws UnexpectedNoAccessRightException {
    assertAuthorizationUser(user, object.getUser());
  }

  private void assertReadAuthorizationPredefinedQuestion(User user, PredefinedQuestion question)
      throws UnexpectedNoAccessRightException {
    assertReadAuthorization(user, question.getNote());
  }

  private void assertReadAuthorizationRecallPrompt(User user, RecallPrompt object)
      throws UnexpectedNoAccessRightException {
    assertReadAuthorizationPredefinedQuestion(user, object.getPredefinedQuestion());
  }

  private void assertAuthorizationNote(User user, Note note)
      throws UnexpectedNoAccessRightException {
    assertLoggedIn(user);
    if (!hasFullAuthority(user, note.getNotebook())) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertReadAuthorizationNote(User user, Note note)
      throws UnexpectedNoAccessRightException {
    assertReadAuthorizationNotebook(user, note.getNotebook());
  }

  private boolean hasFullAuthority(User user, Notebook notebook) {
    if (user == null) return false;
    return user.owns(notebook);
  }

  private void assertReadAuthorizationNotebook(User user, Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (notebook != null) {
      if (user != null && user.canReferTo(notebook)) {
        return;
      }
      if (bazaarNotebookRepository.findByNotebook(notebook) != null) {
        return;
      }
    }
    assertLoggedIn(user);
    throw new UnexpectedNoAccessRightException();
  }

  private void assertAuthorizationNotebook(User user, Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (!hasFullAuthority(user, notebook)) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationCircle(User user, Circle circle)
      throws UnexpectedNoAccessRightException {
    assertLoggedIn(user);
    if (user == null || !user.inCircle(circle)) {
      System.out.printf("user: %s, circle: %s%n", user, circle);
      System.out.printf("user: %s, circle: %s%n", user, circle);
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationSubscription(User user, Subscription subscription)
      throws UnexpectedNoAccessRightException {
    if (subscription.getUser() != user) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  private void assertAuthorizationUser(User user, User targetUser)
      throws UnexpectedNoAccessRightException {
    if (!user.getId().equals(targetUser.getId())) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  public void assertAdminAuthorization() throws UnexpectedNoAccessRightException {
    assertAdminAuthorization(getCurrentUser());
  }

  public void assertAdminAuthorization(User user) throws UnexpectedNoAccessRightException {
    if (!isAdmin(user)) {
      throw new UnexpectedNoAccessRightException();
    }
  }

  public boolean isAdmin() {
    return isAdmin(getCurrentUser());
  }

  public boolean isAdmin(User user) {
    return user != null && user.isAdmin();
  }

  public void assertLoggedIn() {
    assertLoggedIn(getCurrentUser());
  }

  public void assertLoggedIn(User user) {
    if (user == null) {
      throwUserNotFound();
    }
  }

  public static void throwUserNotFound() {
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User Not Found");
  }
}
