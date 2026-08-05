package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptOverlapTryAgainTests extends ControllerTestBase {

  @Autowired RecallPromptController controller;
  @Autowired MemoryTrackerService memoryTrackerService;

  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    Note partnerNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Shared Title").please();
    Note reviewedNote =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .rememberSpelling()
            .title("Shared Title")
            .aliases("color")
            .overlapPartner(partnerNote)
            .please();
    memoryTracker =
        makeMe
            .aMemoryTrackerFor(reviewedNote)
            .forgettingCurveAndNextRecallAt(200.0f)
            .spelling()
            .please();
  }

  private AnsweredQuestion answerSpelling(MemoryTracker tracker, String answer)
      throws UnexpectedNoAccessRightException {
    AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();
    answerDTO.setSpellingAnswer(answer);
    return controller.answerSpelling(
        makeMe.aRecallPrompt().forMemoryTracker(tracker).spelling().please(), answerDTO);
  }

  private void assertCorrectWithRecallCredit(Note reviewed)
      throws UnexpectedNoAccessRightException {
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(reviewed)
            .forgettingCurveAndNextRecallAt(200.0f)
            .spelling()
            .please();
    Integer recallCountBefore = tracker.getRecallCount();

    AnsweredQuestion result = answerSpelling(tracker, reviewed.getTitle());

    assertTrue(result.getAnswer().getCorrect());
    assertThat(tracker.getRecallCount(), equalTo(recallCountBefore + 1));
  }

  @Test
  void shouldGradeAsOverlapWhenAnswerMatchesReviewedAndResolvedOverlapTarget()
      throws UnexpectedNoAccessRightException {
    Integer recallCountBefore = memoryTracker.getRecallCount();
    Float forgettingCurveBefore = memoryTracker.getForgettingCurveIndex();
    Timestamp nextRecallAtBefore = memoryTracker.getNextRecallAt();

    AnsweredQuestion result = answerSpelling(memoryTracker, "Shared Title");

    assertThat(result.getAnswer().getOutcome(), is(AnswerOutcome.OVERLAP));
    assertFalse(result.getAnswer().getCorrect());
    assertThat(memoryTracker.getRecallCount(), equalTo(recallCountBefore));
    assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(forgettingCurveBefore));
    assertThat(memoryTracker.getNextRecallAt(), equalTo(nextRecallAtBefore));
  }

  @Test
  void shouldGradeCorrectWithCreditWhenDistinguishingPlainAlias()
      throws UnexpectedNoAccessRightException {
    Integer recallCountBefore = memoryTracker.getRecallCount();

    AnsweredQuestion result = answerSpelling(memoryTracker, "color");

    assertTrue(result.getAnswer().getCorrect());
    assertThat(memoryTracker.getRecallCount(), equalTo(recallCountBefore + 1));
  }

  @Test
  void shouldNotCountOverlapTowardWrongAnswerThreshold() throws UnexpectedNoAccessRightException {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));

    for (int i = 0; i < 5; i++) {
      answerSpelling(memoryTracker, "Shared Title");
    }
    assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));
  }

  @Test
  void shouldGradeCorrectWithCreditWhenOverlapTargetDoesNotExist()
      throws UnexpectedNoAccessRightException {
    Note reviewed =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .rememberSpelling()
            .title("Reviewed Alone")
            .overlapWikiLink("No Such Notebook:Missing Partner Title")
            .please();
    assertCorrectWithRecallCredit(reviewed);
  }

  @Test
  void shouldGradeCorrectWithCreditWhenOverlapPartnerIsUnreadable()
      throws UnexpectedNoAccessRightException {
    Note partner =
        makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).title("Shared Unreadable").please();
    Note reviewed =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .rememberSpelling()
            .title("Shared Unreadable")
            .overlapPartner(partner)
            .please();
    assertCorrectWithRecallCredit(reviewed);
  }

  @Test
  void shouldGradeCorrectWithCreditWhenOverlapTokenIsSelfReferential()
      throws UnexpectedNoAccessRightException {
    Note reviewed =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .rememberSpelling()
            .title("Self Referential Title")
            .overlapWikiLink("Self Referential Title")
            .please();
    assertCorrectWithRecallCredit(reviewed);
  }
}
