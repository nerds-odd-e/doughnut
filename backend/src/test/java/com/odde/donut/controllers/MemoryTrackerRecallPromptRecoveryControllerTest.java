package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecallPromptRecoveryControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired EntityManager entityManager;

  @Test
  void failedThenSuccessfulMcqLoadingPreservesLearningState()
      throws UnexpectedNoAccessRightException {
    var openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    MemoryTracker tracker = ownedTracker();
    var nextRecallAt = tracker.getNextRecallAt();
    var recallLogs = recallLogsOf(tracker);
    openAiStructuredResponseMock.stubStructuredResponse(null);
    assertThrows(ResponseStatusException.class, () -> controller.getRecallPrompt(tracker));

    openAiStructuredResponseMock.stubStructuredResponse(makeMe.aGeneratedMcq().please());
    var prompt = controller.getRecallPrompt(tracker);
    makeMe.entityPersister.flush();
    makeMe.refresh(tracker);
    RecallPrompt persistedPrompt = entityManager.find(RecallPrompt.class, prompt.getId());
    makeMe.refresh(persistedPrompt);

    assertThat(tracker.getNextRecallAt(), equalTo(nextRecallAt));
    assertThat(recallLogsOf(tracker), equalTo(recallLogs));
    assertThat(persistedPrompt.getAnswer(), nullValue());
    assertThat(controller.getRecallPrompt(tracker).getId(), equalTo(prompt.getId()));
  }

  @Test
  void repeatedSpellingLoadingPreservesUnansweredPromptAndLearningState()
      throws UnexpectedNoAccessRightException {
    var note =
        makeMe
            .aNote("moon")
            .content("partner of earth")
            .notebookOwnedBy(currentUser.getUser())
            .please();
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).spelling().please();
    var nextRecallAt = tracker.getNextRecallAt();
    var recallLogs = recallLogsOf(tracker);

    var prompt = controller.getRecallPrompt(tracker);
    assertThat(controller.getRecallPrompt(tracker).getId(), equalTo(prompt.getId()));
    makeMe.entityPersister.flush();
    makeMe.refresh(tracker);
    RecallPrompt persistedPrompt = entityManager.find(RecallPrompt.class, prompt.getId());
    makeMe.refresh(persistedPrompt);

    assertThat(persistedPrompt.getAnswer(), nullValue());
    assertThat(persistedPrompt.getSpellingQuestion(), notNullValue());
    assertThat(tracker.getNextRecallAt(), equalTo(nextRecallAt));
    assertThat(recallLogsOf(tracker), equalTo(recallLogs));
  }
}
