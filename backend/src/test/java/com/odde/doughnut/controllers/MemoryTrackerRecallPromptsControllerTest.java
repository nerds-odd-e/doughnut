package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.RecallPromptHistoryItem;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecallPromptsControllerTest extends MemoryTrackerControllerTestBase {

  private RecallPrompt answeredPromptFor(MemoryTracker tracker, Note note) {
    return makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(tracker)
        .answerChoiceIndex(0)
        .please();
  }

  @Nested
  class GetRecallPrompts {
    @Test
    void shouldReturnAllRecallPromptsOrderedByIdDesc() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      RecallPrompt prompt1 = promptFor(tracker, note);
      RecallPrompt prompt2 = promptFor(tracker, note);
      RecallPrompt prompt3 = promptFor(tracker, note);

      List<RecallPromptHistoryItem> prompts = controller.getRecallPrompts(tracker);

      assertThat(
          prompts.stream().map(RecallPromptHistoryItem::getId).toList(),
          contains(prompt3.getId(), prompt2.getId(), prompt1.getId()));
    }

    @Test
    void shouldReturnEmptyListWhenNoPrompts() throws UnexpectedNoAccessRightException {
      assertThat(controller.getRecallPrompts(ownedTracker()), empty());
    }

    @Test
    void shouldIncludeBothAnsweredAndUnansweredPrompts() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      RecallPrompt unanswered = promptFor(tracker, note);
      RecallPrompt answered = answeredPromptFor(tracker, note);

      assertThat(
          controller.getRecallPrompts(tracker).stream()
              .map(RecallPromptHistoryItem::getId)
              .toList(),
          containsInAnyOrder(answered.getId(), unanswered.getId()));
    }

    @Test
    void shouldNotBeAbleToGetRecallPromptsForOthersMemoryTracker() {
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getRecallPrompts(memoryTracker));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(ResponseStatusException.class, () -> controller.getRecallPrompts(memoryTracker));
    }
  }

  @Nested
  class DeleteUnansweredRecallPrompts {
    @Test
    void shouldDeleteAllUnansweredRecallPrompts() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      promptFor(tracker, note);
      promptFor(tracker, note);
      RecallPrompt answered = answeredPromptFor(tracker, note);

      controller.deleteUnansweredRecallPrompts(tracker);

      List<RecallPromptHistoryItem> remaining = controller.getRecallPrompts(tracker);
      assertThat(remaining, hasSize(1));
      assertThat(remaining.get(0).getId(), equalTo(answered.getId()));
    }

    @Test
    void shouldDeleteNothingWhenAllPromptsAreAnswered() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      RecallPrompt answered1 = answeredPromptFor(tracker, note);
      RecallPrompt answered2 = answeredPromptFor(tracker, note);

      controller.deleteUnansweredRecallPrompts(tracker);

      assertThat(
          controller.getRecallPrompts(tracker).stream()
              .map(RecallPromptHistoryItem::getId)
              .toList(),
          containsInAnyOrder(answered1.getId(), answered2.getId()));
    }

    @Test
    void shouldDeleteNothingWhenNoPromptsExist() throws UnexpectedNoAccessRightException {
      MemoryTracker tracker = ownedTracker();
      controller.deleteUnansweredRecallPrompts(tracker);
      assertThat(controller.getRecallPrompts(tracker), empty());
    }

    @Test
    void shouldNotDeleteContestedUnansweredRecallPrompts() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      promptFor(tracker, note);
      RecallPrompt contested =
          makeMe
              .aRecallPrompt()
              .withPredefinedQuestionForNote(note)
              .forMemoryTracker(tracker)
              .contested()
              .please();

      controller.deleteUnansweredRecallPrompts(tracker);

      List<RecallPromptHistoryItem> remaining = controller.getRecallPrompts(tracker);
      assertThat(remaining, hasSize(1));
      assertThat(remaining.get(0).getId(), equalTo(contested.getId()));
    }

    @Test
    void shouldNullOutConversationReferenceBeforeDeletingRecallPrompt()
        throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      RecallPrompt unanswered = promptFor(tracker, note);
      Conversation conversation =
          makeMe.aConversation().forARecallPrompt(unanswered).from(currentUser.getUser()).please();

      controller.deleteUnansweredRecallPrompts(tracker);

      makeMe.refresh(conversation);
      assertConversationHasNoRecallPrompt(conversation);
    }

    @Test
    void shouldNotBeAbleToDeleteRecallPromptsForOthersMemoryTracker() {
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.deleteUnansweredRecallPrompts(memoryTracker));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          ResponseStatusException.class,
          () -> controller.deleteUnansweredRecallPrompts(memoryTracker));
    }
  }
}
