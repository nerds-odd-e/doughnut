package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.donut.controllers.dto.AnsweredQuestion;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecallPromptOverlapTryAgainTests extends RecallPromptControllerTestBase {

  MemoryTracker memoryTracker;
  Note partnerNote;

  @BeforeEach
  void setup() {
    partnerNote =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("Partner")
            .aliases("colour", "color", "hue")
            .please();
    Note reviewedNote =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("colour")
            .aliases("color")
            .overlapPartner(partnerNote)
            .please();
    memoryTracker = ownedSpellingTracker(reviewedNote);
  }

  private AnsweredQuestion answerSpelling(MemoryTracker tracker, String answer)
      throws UnexpectedNoAccessRightException {
    return controller.answerSpelling(spellingPrompt(tracker), spellingAnswer(answer));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Partner", "hue"})
  void overlapTitleOrAliasPreservesReviewSchedule(String answer)
      throws UnexpectedNoAccessRightException {
    Float stabilityBefore = memoryTracker.getStability();
    Timestamp nextRecallAtBefore = memoryTracker.getNextRecallAt();
    Timestamp lastRecalledAtBefore = memoryTracker.getLastRecalledAt();

    AnsweredQuestion result = answerSpelling(memoryTracker, answer);

    assertThat(result.getAnswer().getOutcome(), is(AnswerOutcome.OVERLAP));
    assertThat(memoryTracker.getStability(), equalTo(stabilityBefore));
    assertThat(memoryTracker.getNextRecallAt(), equalTo(nextRecallAtBefore));
    assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAtBefore));
  }

  @Test
  void overlapAnswerPreservesRecallLogs() throws UnexpectedNoAccessRightException {
    answerSpelling(memoryTracker, "Partner");

    assertThat(memoryTrackerController.getRecallLogs(memoryTracker), empty());
  }

  @Test
  void overlapAnswerPreservesPartnerReviewState() throws UnexpectedNoAccessRightException {
    MemoryTracker partnerTracker = ownedSpellingTracker(partnerNote);
    float partnerStabilityBefore = partnerTracker.getStability();
    Timestamp partnerDueBefore = partnerTracker.getNextRecallAt();
    int partnerWrongCountBefore =
        memoryTrackerController.getThresholdExceeded(partnerTracker).wrongCount();

    answerSpelling(memoryTracker, "Partner");

    assertThat(memoryTrackerController.getRecallLogs(partnerTracker), empty());
    assertThat(partnerTracker.getStability(), equalTo(partnerStabilityBefore));
    assertThat(partnerTracker.getNextRecallAt(), equalTo(partnerDueBefore));
    assertThat(
        memoryTrackerController.getThresholdExceeded(partnerTracker).wrongCount(),
        equalTo(partnerWrongCountBefore));
  }

  @ParameterizedTest
  @ValueSource(strings = {"colour", "color"})
  void titleOrAliasReceivesCreditWhenSharedWithOverlap(String answer)
      throws UnexpectedNoAccessRightException {
    Integer recallCountBefore = memoryTracker.getRecallCount();

    AnsweredQuestion result = answerSpelling(memoryTracker, answer);

    assertTrue(result.getAnswer().getCorrect());
    assertThat(memoryTracker.getRecallCount(), equalTo(recallCountBefore + 1));
  }

  @Test
  void overlapAnswersPreserveFailureCount() throws UnexpectedNoAccessRightException {
    for (int i = 0; i < 5; i++) {
      answerSpelling(memoryTracker, "Partner");
    }
    assertThat(memoryTrackerController.getThresholdExceeded(memoryTracker).wrongCount(), is(0));
  }
}
