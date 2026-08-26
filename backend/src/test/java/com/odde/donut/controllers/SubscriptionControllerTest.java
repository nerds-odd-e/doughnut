package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.SubscriptionDTO;
import com.odde.donut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.donut.entities.Circle;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGroup;
import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.SubscriptionRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NotebookGroupService;
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
  @Autowired ObjectMapper objectMapper;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    notebook = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please().getNotebook();
    makeMe.aBazaarNotebook(notebook).please();
  }

  @Test
  void createsSubscriptionForBazaarNotebook() throws UnexpectedNoAccessRightException {
    Subscription result = controller.createSubscription(notebook, new SubscriptionDTO());
    assertEquals(notebook.getId(), result.getNotebook().getId());
    assertEquals(currentUser.getUser(), result.getUser());
  }

  @Test
  void createdSubscriptionForCircleNotebookSerializesWithoutLegacyNotebookFields()
      throws Exception {
    Circle circle = makeMe.aCircle().hasMember(currentUser.getUser()).please();
    Notebook circleNotebook =
        makeMe.refresh(makeMe.aNote("Circle notebook").inCircle(circle).please()).getNotebook();

    SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
    subscriptionDTO.setDailyTargetOfNewNotes(1);
    Subscription result = controller.createSubscription(circleNotebook, subscriptionDTO);

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(result));
    assertThat(json.has("headNote"), equalTo(false));
    assertThat(json.get("dailyTargetOfNewNotes").asInt(), equalTo(1));
  }

  @Test
  void deniesNonBazaarNotebook() {
    Note anotherNote = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.createSubscription(anotherNote.getNotebook(), new SubscriptionDTO()));
  }

  @Test
  void rejectsSubscribeWhenSkipMemoryTracking() {
    makeMe.theNotebook(notebook).skipMemoryTrackingEntirely(true).please();
    long before = subscriptionRepository.count();
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.createSubscription(notebook, new SubscriptionDTO()));
    assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(subscriptionRepository.count(), equalTo(before));
  }

  @Nested
  class Unsubscribe {
    @Test
    void removesSubscription() throws UnexpectedNoAccessRightException {
      Subscription subscription = makeMe.aSubscription().forUser(currentUser.getUser()).please();
      long beforeDestroy = subscriptionRepository.count();
      controller.destroySubscription(subscription);
      assertThat(subscriptionRepository.count(), equalTo(beforeDestroy - 1));
    }

    @Test
    void deniesOtherUsersSubscription() {
      Subscription subscription = makeMe.aSubscription().forUser(makeMe.aUser().please()).please();
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
      User owner = makeMe.aUser().please();
      Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(bazaarNotebook).please();
      subscription =
          makeMe
              .aSubscription()
              .forNotebook(bazaarNotebook)
              .forUser(currentUser.getUser())
              .please();
    }

    @Test
    void assignsSubscriptionToGroup() throws UnexpectedNoAccessRightException {
      User subscriber = currentUser.getUser();
      NotebookGroup group =
          notebookGroupService.createGroup(subscriber, subscriber.getOwnership(), "G");
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(group.getId());
      assertThat(
          controller.updateSubscriptionGroup(subscription, req).getNotebookGroup().getId(),
          equalTo(group.getId()));
    }

    @Test
    void clearsGroupWhenNotebookGroupIdIsNull() throws UnexpectedNoAccessRightException {
      User subscriber = currentUser.getUser();
      NotebookGroup group =
          notebookGroupService.createGroup(subscriber, subscriber.getOwnership(), "G");
      notebookGroupService.assignSubscriptionToGroup(subscriber, subscription, group);
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(null);
      assertThat(
          controller.updateSubscriptionGroup(subscription, req).getNotebookGroup(), nullValue());
    }

    @Test
    void rejectsGroupFromAnotherOwnership() {
      NotebookGroup otherGroup =
          makeMe.aNotebookGroup().ownership(makeMe.aUser().please().getOwnership()).please();
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
