package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.RecallQuestion;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerAskQuestionControllerTest extends MemoryTrackerControllerTestBase {

  private MemoryTracker spellingTracker() {
    Note note =
        makeMe
            .aNote("moon")
            .content("partner of earth")
            .notebookOwnedBy(currentUser.getUser())
            .please();
    return makeMe.aMemoryTrackerFor(note).spelling().please();
  }

  @Test
  void shouldReturnSpellingRecallPromptForSpellingMemoryTracker()
      throws UnexpectedNoAccessRightException {
    RecallQuestion recallQuestion = controller.askAQuestion(spellingTracker());
    assertThat(recallQuestion.getSpellingQuestion(), notNullValue());
    assertThat(recallQuestion.getMultipleChoicesQuestion(), nullValue());
  }

  @Test
  void shouldRecycleUnansweredSpellingRecallPromptForSpellingMemoryTracker()
      throws UnexpectedNoAccessRightException {
    MemoryTracker memoryTracker = spellingTracker();
    RecallPrompt existingPrompt =
        makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();

    assertThat(controller.askAQuestion(memoryTracker).getId(), equalTo(existingPrompt.getId()));
  }

  @Test
  void shouldNotBeAbleToAskQuestionForOthersMemoryTracker() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.askAQuestion(memoryTracker));
  }

  @Test
  void shouldRequireUserToBeLoggedIn() {
    currentUser.setUser(null);
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(ResponseStatusException.class, () -> controller.askAQuestion(memoryTracker));
  }

  @Test
  void shouldThrowWhenOpenAiNotAvailableAndGeneratingQuestion() {
    MemoryTracker memoryTracker = ownedTracker();
    testabilitySettings.setOpenAiTokenOverride("");
    assertThrows(OpenAiNotAvailableException.class, () -> controller.askAQuestion(memoryTracker));
  }
}
