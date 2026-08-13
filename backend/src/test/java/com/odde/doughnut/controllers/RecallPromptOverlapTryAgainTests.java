package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import com.odde.doughnut.entities.AnswerOutcome;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptOverlapTryAgainTests extends RecallPromptControllerTestBase {

  @Autowired MemoryTrackerService memoryTrackerService;

  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    Note partnerNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Shared Title").please();
    Note reviewedNote =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("Shared Title")
            .aliases("color")
            .overlapPartner(partnerNote)
            .please();
    memoryTracker = ownedSpellingTracker(reviewedNote);
  }

  private AnsweredQuestion answerSpelling(MemoryTracker tracker, String answer)
      throws UnexpectedNoAccessRightException {
    return controller.answerSpelling(spellingPrompt(tracker), spellingAnswer(answer));
  }

  private void assertCorrectWithRecallCredit(Note reviewed)
      throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedSpellingTracker(reviewed);
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
    Timestamp lastRecalledAtBefore = memoryTracker.getLastRecalledAt();

    AnsweredQuestion result = answerSpelling(memoryTracker, "Shared Title");

    assertThat(result.getAnswer().getOutcome(), is(AnswerOutcome.OVERLAP));
    assertFalse(result.getAnswer().getCorrect());
    assertThat(memoryTracker.getRecallCount(), equalTo(recallCountBefore));
    assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(forgettingCurveBefore));
    assertThat(memoryTracker.getNextRecallAt(), equalTo(nextRecallAtBefore));
    assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAtBefore));
  }

  @Test
  void shouldNotGradeAsOverlapWhenWikiLinkOnlyUnderAliases()
      throws UnexpectedNoAccessRightException {
    Note partner =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Shared Legacy").please();
    Note reviewed =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("Shared Legacy")
            .wikiLinkUnderAliasesPartner(partner)
            .please();

    AnsweredQuestion result = answerSpelling(ownedSpellingTracker(reviewed), "Shared Legacy");

    assertThat(result.getAnswer().getOutcome(), is(not(AnswerOutcome.OVERLAP)));
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
            .title("Self Referential Title")
            .overlapWikiLink("Self Referential Title")
            .please();
    assertCorrectWithRecallCredit(reviewed);
  }
}
