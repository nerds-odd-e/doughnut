package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.entities.AnswerOutcome;
import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.utils.TimestampOperations;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptAccidentalMatchEdgeTests extends RecallPromptControllerTestBase {

  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired EntityManager entityManager;

  Note answerNote;
  MemoryTracker memoryTracker;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO;

  @BeforeEach
  void setup() {
    answerNote = ownedNote();
    memoryTracker = ownedSpellingTracker(answerNote);
    recallPrompt = spellingPrompt(memoryTracker);
    Note secondNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Another Note Title").please();
    answerDTO = spellingAnswer(secondNote.getTitle());
  }

  @Test
  void shouldGradeBlankAnswerAsPlainWrongEvenWhenEmptyTitleReadableNoteExists()
      throws UnexpectedNoAccessRightException {
    Note emptyTitleNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("temp").please();
    entityManager
        .createQuery("UPDATE Note n SET n.title = '' WHERE n.id = :id")
        .setParameter("id", emptyTitleNote.getId())
        .executeUpdate();
    entityManager.flush();
    answerDTO = spellingAnswer("");

    assertNull(controller.answerSpelling(recallPrompt, answerDTO).getAnswer().getOutcome());
  }

  @Test
  void shouldGradeWhitespaceOnlyAnswerAsPlainWrong() throws UnexpectedNoAccessRightException {
    answerDTO = spellingAnswer("   \t  ");

    assertNull(controller.answerSpelling(recallPrompt, answerDTO).getAnswer().getOutcome());
  }

  @Test
  void shouldApplyOrdinaryIncorrectFailureScheduleOnAccidentalMatch()
      throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    var gradeTime = testabilitySettings.getCurrentUTCTimestamp();
    Integer oldRecallCount = memoryTracker.getRecallCount();
    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(memoryTracker.getRecallCount(), equalTo(oldRecallCount + 1));
    assertThat(memoryTracker.getLastRecalledAt(), equalTo(gradeTime));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(TimestampOperations.addHoursToTimestamp(gradeTime, 12)));
  }

  @Test
  void shouldNotLeakMatchedNoteIdFromUnreadableNotebook() throws UnexpectedNoAccessRightException {
    Note unreadableNote =
        makeMe
            .aNote()
            .notebookOwnedBy(makeMe.aUser().please())
            .title("Unreadable Accidental Title")
            .please();
    answerDTO = spellingAnswer(unreadableNote.getTitle());

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertNull(answerResult.getAnswer().getOutcome());
    assertNull(answerResult.getAnswer().getMatchedNoteId());
  }

  @Test
  void shouldOmitUnreadableNotesFromMatchedNotesWhenReadableMatchAlsoExists()
      throws UnexpectedNoAccessRightException {
    String sharedTitle = "Shared Accidental Title";
    makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).title(sharedTitle).please();
    Note readableNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(sharedTitle).please();
    answerDTO = spellingAnswer(sharedTitle);

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(
        answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
        contains(readableNote.getId()));
  }

  @Test
  void shouldSkipAccidentalMatchSearchWhenAnswerMatchesReviewedNoteEvenIfAnotherNoteSharesTitle()
      throws UnexpectedNoAccessRightException {
    makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(answerNote.getTitle()).please();
    answerDTO = spellingAnswer(answerNote.getTitle());

    assertTrue(controller.answerSpelling(recallPrompt, answerDTO).getAnswer().getCorrect());
  }

  @Test
  void shouldNotDropPromptedTrackerBelowStabilityFloorOnAccidentalMatch()
      throws UnexpectedNoAccessRightException {
    memoryTracker.setStability(ForgettingCurve.ASSIMILATE_STABILITY_HOURS);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    makeMe.entityPersister.save(memoryTracker);
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(memoryTracker.getStability(), equalTo(ForgettingCurve.ASSIMILATE_STABILITY_HOURS));
  }

  @Test
  void shouldStillCountAccidentalMatchTowardWrongAnswerThreshold()
      throws UnexpectedNoAccessRightException {
    var now = testabilitySettings.getCurrentUTCTimestamp();

    for (int i = 0; i < 4; i++) {
      controller.answerSpelling(spellingPrompt(memoryTracker), answerDTO);
    }
    assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));

    AnsweredQuestion lastAnswer =
        controller.answerSpelling(spellingPrompt(memoryTracker), answerDTO);
    assertThat(lastAnswer.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
    assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(true));
  }
}
