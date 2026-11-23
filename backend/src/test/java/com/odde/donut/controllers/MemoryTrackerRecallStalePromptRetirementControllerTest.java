package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.services.FocusNoteFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecallStalePromptRetirementControllerTest
    extends MemoryTrackerBatchImportControllerTestBase {
  private StaleWaitingQueue queue;

  @BeforeEach
  void prepareAStaleWaitingQueue() throws Exception {
    queue = prepareAStaleWaitingQueueAfterAnAcceptedEdit();
    makeMe.entityPersister.flushAndClear();
  }

  @Nested
  class WhenRecallGeneratesTheCurrentQuestion {
    private int current;

    @BeforeEach
    void openRecall() throws Exception {
      synchronousApi.stubStructuredResponse(
          makeMe.aGeneratedMcq().stem("When does it open now?").correctAnswer("Ten").please());
      current = controller.getRecallPrompt(reloadTracker()).getId();
      makeMe.entityPersister.flushAndClear();
    }

    @Test
    void offersAQuestionFromTheEditedFocus() {
      assertThat(
          entityManager.find(RecallPrompt.class, current).getMcq().getFocusNoteFingerprint(),
          is(FocusNoteFingerprint.of(reloadTracker().getNote())));
    }

    @Test
    void retiresOnlyTheUnansweredUncontestedPromptsFromAnotherFocus() {
      assertThat(
          savedPrompts().stream().map(RecallPrompt::getId).toList(),
          contains(current, queue.contested().getId(), queue.answered().getId()));
    }

    @Test
    void keepsTheEarlierAnswerWithItsChoicesAndGrade() throws Exception {
      assertThat(
          controller.getRecallHistory(reloadTracker()),
          hasItem(
              allOf(
                  hasProperty("recallLog", hasProperty("grade", is(queue.grade()))),
                  hasProperty(
                      "recallPrompt",
                      allOf(
                          hasProperty("answer", hasProperty("choiceIndex", is(0))),
                          hasProperty(
                              "mcq",
                              hasProperty(
                                  "responseChoices",
                                  is(queue.answered().getMcq().getResponseChoices()))))))));
    }

    @Test
    void keepsTheTrackerDueTime() {
      assertThat(reloadTracker().getNextRecallAt(), is(queue.nextRecallAt()));
    }
  }

  @Test
  void retiresStalePromptsWhenRecallSelectsAWaitingCurrentQuestion() throws Exception {
    var current =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(tracker)
            .generatedFrom(reloadTracker().getNote())
            .please();
    makeMe.entityPersister.flushAndClear();

    assertThat(controller.getRecallPrompt(reloadTracker()).getId(), is(current.getId()));
    makeMe.entityPersister.flushAndClear();
    assertThat(
        savedPrompts().stream().map(RecallPrompt::getId).toList(),
        contains(current.getId(), queue.contested().getId(), queue.answered().getId()));
  }

  @Test
  void keepsStalePromptsWhenRecallHasNoCurrentQuestion() {
    assertThrows(ResponseStatusException.class, () -> controller.getRecallPrompt(reloadTracker()));
    makeMe.entityPersister.flushAndClear();
    assertThat(
        savedPrompts().stream().map(RecallPrompt::getId).toList(), hasItem(queue.stale().getId()));
  }
}
