package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.RecallPromptHistoryItem;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecallPromptsControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired ObjectMapper objectMapper;

  @Nested
  class GetRecallPrompts {
    @Test
    void shouldReturnAllRecallPromptsOrderedByIdDesc() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      RecallPrompt prompt1 = promptFor(tracker);
      RecallPrompt prompt2 = promptFor(tracker);
      RecallPrompt prompt3 = promptFor(tracker);

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
      RecallPrompt unanswered = promptFor(tracker);
      RecallPrompt answered = answeredPromptFor(tracker);

      assertThat(
          controller.getRecallPrompts(tracker).stream()
              .map(RecallPromptHistoryItem::getId)
              .toList(),
          containsInAnyOrder(answered.getId(), unanswered.getId()));
    }

    @Test
    void shouldOmitMcqSolutionFromUnansweredHistory() throws Exception {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      promptFor(tracker);

      JsonNode json =
          objectMapper.readTree(
              objectMapper.writeValueAsString(controller.getRecallPrompts(tracker)));
      JsonNode mcqJson = json.get(0).get("mcq");
      assertThat(mcqJson.has("questionStem"), is(true));
      assertThat(mcqJson.has("responseChoices"), is(true));
      assertThat(mcqJson.has("correctAnswerIndex"), is(false));
      assertThat(mcqJson.has("testedFocus"), is(false));
      assertThat(mcqJson.has("validationRationale"), is(false));
    }

    @Test
    void shouldIncludeMcqSolutionOnAnsweredHistory() throws Exception {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      answeredPromptFor(tracker);

      JsonNode json =
          objectMapper.readTree(
              objectMapper.writeValueAsString(controller.getRecallPrompts(tracker)));
      assertThat(json.get(0).get("mcq").has("correctAnswerIndex"), is(true));
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
      promptFor(tracker);
      promptFor(tracker);
      RecallPrompt answered = answeredPromptFor(tracker);

      controller.deleteUnansweredRecallPrompts(tracker);

      List<RecallPromptHistoryItem> remaining = controller.getRecallPrompts(tracker);
      assertThat(remaining, hasSize(1));
      assertThat(remaining.get(0).getId(), equalTo(answered.getId()));
    }

    @Test
    void shouldDeleteNothingWhenAllPromptsAreAnswered() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      MemoryTracker tracker = ownedTracker(note);
      RecallPrompt answered1 = answeredPromptFor(tracker);
      RecallPrompt answered2 = answeredPromptFor(tracker);

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
      promptFor(tracker);
      RecallPrompt contested =
          makeMe.aRecallPrompt().forMemoryTracker(tracker).contested().please();

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
      RecallPrompt unanswered = promptFor(tracker);
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
