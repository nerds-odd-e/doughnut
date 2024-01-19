package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.SubscriptionRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestSubscriptionControllerTest {
  @Autowired private MakeMe makeMe;
  @Autowired private SubscriptionRepository subscriptionRepository;
  private UserModel userModel;
  private Note topNote;
  private Notebook notebook;
  private RestSubscriptionController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    topNote = makeMe.aNote().creatorAndOwner(userModel).please();
    notebook = topNote.getNotebook();
    makeMe.aBazaarNodebook(topNote.getNotebook()).please();
    controller = new RestSubscriptionController(makeMe.modelFactoryService, userModel);
  }

  @Test
  void subscribeToNoteSuccessfully() throws UnexpectedNoAccessRightException {
    Subscription subscription = makeMe.aSubscription().inMemoryPlease();
    Subscription result = controller.createSubscription(notebook, subscription);
    assertEquals(topNote, result.getHeadNote());
    assertEquals(userModel.getEntity(), result.getUser());
  }

  @Test
  void notAllowToSubscribeToNoneBazaarNote() {
    Note anotherNote = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
    Subscription subscription = makeMe.aSubscription().inMemoryPlease();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.createSubscription(anotherNote.getNotebook(), subscription));
  }

  @Nested
  class Unsubscribe {
    @Test
    void shouldRemoveTheSubscription() throws UnexpectedNoAccessRightException {
      Subscription subscription = makeMe.aSubscription().forUser(userModel.getEntity()).please();
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
