package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptAccidentalMatchConfusionAdjustmentTests extends RecallPromptControllerTestBase {

  @Autowired MemoryTrackerService memoryTrackerService;

  MemoryTracker promptedTracker;
  MemoryTracker matchedSpellingTracker;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO;

  @BeforeEach
  void setup() {
    promptedTracker = ownedSpellingTracker(ownedNote());
    recallPrompt = spellingPrompt(promptedTracker);
    Note matchedNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Another Note Title").please();
    matchedSpellingTracker = ownedSpellingTracker(matchedNote);
    answerDTO = spellingAnswer(matchedNote.getTitle());
  }

  @Test
  void shouldWeakenUniqueMatchedSpellingTrackerWithoutRecallCredit()
      throws UnexpectedNoAccessRightException {
    float strengthBefore = matchedSpellingTracker.getForgettingCurveIndex();
    Timestamp dueBefore = matchedSpellingTracker.getNextRecallAt();
    Timestamp lastRecalledBefore = matchedSpellingTracker.getLastRecalledAt();
    Integer recallCountBefore = matchedSpellingTracker.getRecallCount();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    int wrongCountBefore =
        memoryTrackerService.countWrongAnswersInPeriod(matchedSpellingTracker, now, 14);

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(
        matchedSpellingTracker.getForgettingCurveIndex(),
        equalTo(strengthBefore - ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT));
    assertThat(matchedSpellingTracker.getLastRecalledAt(), equalTo(lastRecalledBefore));
    assertThat(matchedSpellingTracker.getRecallCount(), equalTo(recallCountBefore));
    assertThat(
        matchedSpellingTracker.getNextRecallAt(),
        equalTo(matchedSpellingTracker.calculateNextRecallAt()));
    assertFalse(matchedSpellingTracker.getNextRecallAt().after(dueBefore));
    assertThat(
        memoryTrackerService.countWrongAnswersInPeriod(matchedSpellingTracker, now, 14),
        equalTo(wrongCountBefore));
    assertThat(
        recallPrompt.getAnswer().getConfusionAdjustedMemoryTracker().getId(),
        equalTo(matchedSpellingTracker.getId()));
  }

  @Test
  void shouldNotDropMatchedSpellingTrackerBelowStrengthFloor()
      throws UnexpectedNoAccessRightException {
    matchedSpellingTracker.setForgettingCurveIndex(ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX);
    matchedSpellingTracker.setNextRecallAt(matchedSpellingTracker.calculateNextRecallAt());
    makeMe.entityPersister.save(matchedSpellingTracker);

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(
        matchedSpellingTracker.getForgettingCurveIndex(),
        equalTo(ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX));
  }

  @Test
  void shouldNotPostponeAlreadyDueMatchedSpellingTracker() throws UnexpectedNoAccessRightException {
    Timestamp alreadyDue = testabilitySettings.getCurrentUTCTimestamp();
    matchedSpellingTracker.setNextRecallAt(alreadyDue);
    makeMe.entityPersister.save(matchedSpellingTracker);

    controller.answerSpelling(recallPrompt, answerDTO);

    assertFalse(matchedSpellingTracker.getNextRecallAt().after(alreadyDue));
  }
}
