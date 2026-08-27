package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.AnswerSpellingDTO;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class MemoryTrackerRecallHistoryRetrievabilityTest extends RecallPromptControllerTestBase {

  @Test
  void gradingPopulatesPreGradeSnapshotOnTheRecallLog() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker(ownedNote());
    Float stabilityBefore = tracker.getStability();
    Float difficultyBefore = tracker.getDifficulty();

    memoryTrackerController.markAsRecalled(tracker, Grade.GOOD);

    RecallLog log = memoryTrackerController.getRecallLogs(tracker).get(0);
    assertThat(log.getStabilityBefore(), equalTo(stabilityBefore));
    assertThat(log.getDifficultyBefore(), equalTo(difficultyBefore));
    assertThat(log.getRetrievability(), notNullValue());
    assertThat(tracker.getStability(), is(not(stabilityBefore)));
  }

  @Test
  void gradingANewTrackerForTheFirstTimeDoesNotProduceNaNRetrievability()
      throws UnexpectedNoAccessRightException {
    MemoryTracker newTracker = makeMe.aMemoryTrackerFor(ownedNote()).please();

    memoryTrackerController.markAsRecalled(newTracker, Grade.GOOD);

    RecallLog log = memoryTrackerController.getRecallLogs(newTracker).get(0);
    assertThat(log.getStabilityBefore(), equalTo(0f));
    assertThat(log.getRetrievability(), nullValue());
  }

  @Test
  void confusionAdjustmentPopulatesPreAdjustmentSnapshotOnTheRecallLog()
      throws UnexpectedNoAccessRightException {
    MemoryTracker promptedTracker = ownedSpellingTracker(ownedNote());
    RecallPrompt recallPrompt = spellingPrompt(promptedTracker);
    Note matchedNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Another Note Title").please();
    MemoryTracker matchedSpellingTracker = ownedSpellingTracker(matchedNote);
    Float stabilityBefore = matchedSpellingTracker.getStability();
    Float difficultyBefore = matchedSpellingTracker.getDifficulty();
    AnswerSpellingDTO answerDTO = spellingAnswer(matchedNote.getTitle());

    controller.answerSpelling(recallPrompt, answerDTO);

    RecallLog matchedLog = memoryTrackerController.getRecallLogs(matchedSpellingTracker).get(0);
    assertThat(matchedLog.isConfusion(), is(true));
    assertThat(matchedLog.getStabilityBefore(), equalTo(stabilityBefore));
    assertThat(matchedLog.getDifficultyBefore(), equalTo(difficultyBefore));
    assertThat(matchedLog.getRetrievability(), notNullValue());
    assertThat(matchedSpellingTracker.getStability(), is(not(stabilityBefore)));
  }
}
