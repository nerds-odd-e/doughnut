package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Grade;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.testability.builders.RecallPromptBuilder;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryTrackerBatchStalePromptRetirementControllerTest
    extends MemoryTrackerBatchImportControllerTestBase {
  private RecallPrompt answered;
  private RecallPrompt contested;
  private RecallPrompt unknownInput;
  private Grade grade;
  private Timestamp nextRecallAt;
  private RecallPrompt imported;

  @BeforeEach
  void importAReplacementForAStaleWaitingQueue() throws Exception {
    answered = promptFromFocus().answerChoiceIndex(0).recorded().please();
    contested = promptFromFocus().contested().please();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).withMcq(answered.getMcq()).please();
    promptFromFocus().please();
    unknownInput = makeMe.aRecallPrompt().forMemoryTracker(tracker).please();
    makeMe.entityPersister.flushAndClear();
    grade = Grade.fromCorrect(answered.getAnswer().getCorrect());
    nextRecallAt = reloadTracker().getNextRecallAt();
    publishCorrectedFocus();

    imported = importProviderQuestion(renderRequestMetadata());
  }

  @Test
  void retiresOnlyTheUnansweredUncontestedPromptsFromAnotherFocus() {
    assertThat(
        savedPrompts().stream().map(RecallPrompt::getId).toList(),
        contains(imported.getId(), unknownInput.getId(), contested.getId(), answered.getId()));
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
                hasProperty("recallLog", hasProperty("grade", is(grade))),
                hasProperty(
                    "recallPrompt", hasProperty("answer", hasProperty("choiceIndex", is(0)))))));
  }

  @Test
  void keepsTheTrackerDueTime() {
    assertThat(reloadTracker().getNextRecallAt(), is(nextRecallAt));
  }

  private RecallPromptBuilder promptFromFocus() {
    return makeMe.aRecallPrompt().forMemoryTracker(tracker).generatedFrom(tracker.getNote());
  }
}
