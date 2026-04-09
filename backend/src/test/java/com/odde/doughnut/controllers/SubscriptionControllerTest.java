package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.SubscriptionDTO;
import com.odde.doughnut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.SubscriptionRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NotebookGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SubscriptionControllerTest extends ControllerTestBase {
  @Autowired private SubscriptionRepository subscriptionRepository;
  @Autowired SubscriptionController controller;
  @Autowired NotebookGroupService notebookGroupService;
  private Note topNote;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    topNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
    notebook = topNote.getNotebook();
    makeMe.aBazaarNotebook(topNote.getNotebook()).please();
  }

  @Test
  void subscribeToNoteSuccessfully() throws UnexpectedNoAccessRightException {
    SubscriptionDTO subscription = new SubscriptionDTO();
    Subscription result = controller.createSubscription(notebook, subscription);
    assertEquals(topNote, result.getHeadNote());
    assertEquals(currentUser.getUser(), result.getUser());
  }

  @Test
  void notAllowToSubscribeToNoneBazaarNote() {
    Note anotherNote = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
    SubscriptionDTO subscription = new SubscriptionDTO();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.createSubscription(anotherNote.getNotebook(), subscription));
  }

  @Nested
  class Unsubscribe {
    @Test
    void shouldRemoveTheSubscription() throws UnexpectedNoAccessRightException {
      Subscription subscription = makeMe.aSubscription().forUser(currentUser.getUser()).please();
      long beforeDestroy = subscriptionRepository.count();
      controller.destroySubscription(subscription);
      assertThat(subscriptionRepository.count(), equalTo(beforeDestroy - 1));
    }

    @Test
    void notAllowToUnsubscribeForOtherPeople() {
      User anotherUser = makeMe.aUser().please();
      Subscription subscription = makeMe.aSubscription().forUser(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.destroySubscription(subscription));
    }
  }

  @Nested
  class UpdateSubscriptionGroup {
    private Subscription subscription;

    @BeforeEach
    void setup() {
      User subscriber = currentUser.getUser();
      User owner = makeMe.aUser().please();
      Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(bazaarNotebook).please();
      subscription =
          makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(subscriber).please();
    }

    @Test
    void assignsSubscriptionToGroup() throws UnexpectedNoAccessRightException {
      User subscriber = currentUser.getUser();
      NotebookGroup group =
          notebookGroupService.createGroup(subscriber, subscriber.getOwnership(), "G");
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(group.getId());
      Subscription result = controller.updateSubscriptionGroup(subscription, req);
      assertThat(result.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void clearsGroupWhenNotebookGroupIdIsNull() throws UnexpectedNoAccessRightException {
      User subscriber = currentUser.getUser();
      NotebookGroup group =
          notebookGroupService.createGroup(subscriber, subscriber.getOwnership(), "G");
      notebookGroupService.assignSubscriptionToGroup(subscriber, subscription, group);
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(null);
      Subscription result = controller.updateSubscriptionGroup(subscription, req);
      assertThat(result.getNotebookGroup(), nullValue());
    }

    @Test
    void rejectsGroupFromAnotherOwnership() {
      User other = makeMe.aUser().please();
      NotebookGroup otherGroup = makeMe.aNotebookGroup().ownership(other.getOwnership()).please();
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(otherGroup.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateSubscriptionGroup(subscription, req));
    }

    @Test
    void notFoundWhenGroupDoesNotExist() {
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(9_999_999);
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.updateSubscriptionGroup(subscription, req));
      assertThat(ex.getStatusCode().value(), equalTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void rejectsSubscriptionOwnedByAnotherUser() {
      User other = makeMe.aUser().please();
      User owner = makeMe.aUser().please();
      Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(bazaarNotebook).please();
      Subscription otherSubscription =
          makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(other).please();
      NotebookGroup group =
          makeMe.aNotebookGroup().ownership(currentUser.getUser().getOwnership()).please();
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(group.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateSubscriptionGroup(otherSubscription, req));
    }
  }
}
