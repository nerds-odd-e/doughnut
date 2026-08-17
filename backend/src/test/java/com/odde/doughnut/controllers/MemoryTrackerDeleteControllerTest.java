package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.repositories.ConversationRepository;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerDeleteControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired ConversationRepository conversationRepository;

  @Test
  void shouldHardDeleteMemoryTracker() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).propertyKey("topic").please();

    controller.delete(tracker);

    assertThat(memoryTrackerRepository.findById(tracker.getId()).isEmpty(), is(true));
  }

  @Test
  void shouldHardDeleteMemoryTrackerWhenItsRecallPromptHasAConversation()
      throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    RecallPrompt recallPrompt = promptFor(tracker, note);
    Conversation conversation =
        makeMe.aConversation().forARecallPrompt(recallPrompt).from(currentUser.getUser()).please();
    makeMe.aRecallLogFor(tracker).please();

    Integer trackerId = tracker.getId();
    Integer conversationId = conversation.getId();
    makeMe.entityPersister.flushAndClear();

    controller.delete(memoryTrackerRepository.findById(trackerId).orElseThrow());

    assertThat(memoryTrackerRepository.findById(trackerId).isEmpty(), is(true));
    assertConversationHasNoRecallPrompt(
        conversationRepository.findById(conversationId).orElseThrow());
  }

  @Test
  void shouldNotBeAbleToDeleteOthersMemoryTracker() {
    MemoryTracker tracker =
        makeMe.aMemoryTrackerBy(makeMe.aUser().please()).propertyKey("topic").please();

    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.delete(tracker));
  }

  @Test
  void shouldRequireUserToBeLoggedIn() {
    currentUser.setUser(null);
    MemoryTracker tracker =
        makeMe.aMemoryTrackerBy(makeMe.aUser().please()).propertyKey("topic").please();

    assertThrows(ResponseStatusException.class, () -> controller.delete(tracker));
  }
}
