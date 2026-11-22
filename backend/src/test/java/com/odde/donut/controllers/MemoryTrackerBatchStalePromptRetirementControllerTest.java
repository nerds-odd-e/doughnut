package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.RecallPrompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryTrackerBatchStalePromptRetirementControllerTest
    extends MemoryTrackerBatchImportControllerTestBase {
  private StaleWaitingQueue queue;
  private RecallPrompt imported;

  @BeforeEach
  void importAReplacementForAStaleWaitingQueue() throws Exception {
    queue = prepareAStaleWaitingQueueAfterAnAcceptedEdit();
    imported = importProviderQuestion(renderRequestMetadata());
  }

  @Test
  void retiresOnlyTheUnansweredUncontestedPromptsFromAnotherFocus() {
    assertThat(
        savedPrompts().stream().map(RecallPrompt::getId).toList(),
        contains(
            imported.getId(),
            queue.unknownInput().getId(),
            queue.contested().getId(),
            queue.answered().getId()));
  }

  @Test
  void offersTheImportedReplacementAtRecall() throws Exception {
    assertThat(controller.getRecallPrompt(reloadTracker()).getId(), is(imported.getId()));
  }

  @Test
  void keepsTheEarlierAnswerAndItsGrade() throws Exception {
    assertThat(
        controller.getRecallHistory(reloadTracker()),
        hasItem(
            allOf(
                hasProperty("recallLog", hasProperty("grade", is(queue.grade()))),
                hasProperty(
                    "recallPrompt", hasProperty("answer", hasProperty("choiceIndex", is(0)))))));
  }

  @Test
  void keepsTheTrackerDueTime() {
    assertThat(reloadTracker().getNextRecallAt(), is(queue.nextRecallAt()));
  }
}
