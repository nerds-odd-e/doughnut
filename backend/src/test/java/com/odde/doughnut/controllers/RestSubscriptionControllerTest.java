package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.SubscriptionDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.SubscriptionRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionControllerTest extends ControllerTestBase {
  @Autowired private SubscriptionRepository subscriptionRepository;
  private Note topNote;
  private Notebook notebook;
  private SubscriptionController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    topNote =
        makeMe
            .aNote()
            .creatorAndOwner(makeMe.modelFactoryService.toUserModel(currentUser.getUser()))
            .please();
    notebook = topNote.getNotebook();
    makeMe.aBazaarNotebook(topNote.getNotebook()).please();
    controller = new SubscriptionController(makeMe.modelFactoryService, authorizationService);
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
}
