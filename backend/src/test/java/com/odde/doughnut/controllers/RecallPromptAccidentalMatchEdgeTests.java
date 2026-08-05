package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.utils.TimestampOperations;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptAccidentalMatchEdgeTests extends ControllerTestBase {

  @Autowired RecallPromptController controller;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired EntityManager entityManager;

  Note answerNote;
  Note secondNote;
  MemoryTracker memoryTracker;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    answerNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).rememberSpelling().please();
    memoryTracker =
        makeMe
            .aMemoryTrackerFor(answerNote)
            .forgettingCurveAndNextRecallAt(200.0f)
            .spelling()
            .please();
    recallPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
    secondNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Another Note Title").please();
    answerDTO.setSpellingAnswer(secondNote.getTitle());
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
    answerDTO.setSpellingAnswer("");

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertNull(answerResult.getAnswer().getOutcome());
  }

  @Test
  void shouldGradeWhitespaceOnlyAnswerAsPlainWrong() throws UnexpectedNoAccessRightException {
    answerDTO.setSpellingAnswer("   \t  ");

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertNull(answerResult.getAnswer().getOutcome());
  }

  @Test
  void shouldApplyLighterPenaltyThanWrongAnswer() throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(190.0f));
    assertThat(
        memoryTracker.getNextRecallAt(), greaterThan(testabilitySettings.getCurrentUTCTimestamp()));
    assertThat(
        memoryTracker.getNextRecallAt(),
        not(
            equalTo(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 12))));
  }

  @Test
  void shouldNotLeakMatchedNoteIdFromUnreadableNotebook() throws UnexpectedNoAccessRightException {
    Note unreadableNote =
        makeMe
            .aNote()
            .notebookOwnedBy(makeMe.aUser().please())
            .title("Unreadable Accidental Title")
            .please();
    answerDTO.setSpellingAnswer(unreadableNote.getTitle());

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
    answerDTO.setSpellingAnswer(sharedTitle);

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(
        answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
        contains(readableNote.getId()));
  }

  @Test
  void shouldSkipAccidentalMatchSearchWhenAnswerMatchesReviewedNoteEvenIfAnotherNoteSharesTitle()
      throws UnexpectedNoAccessRightException {
    makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(answerNote.getTitle()).please();
    answerDTO.setSpellingAnswer(answerNote.getTitle());

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertTrue(answerResult.getAnswer().getCorrect());
    assertNull(answerResult.getAnswer().getOutcome());
  }

  @Test
  void shouldNotDropForgettingCurveIndexBelowFloorOnAccidentalMatch()
      throws UnexpectedNoAccessRightException {
    memoryTracker.setForgettingCurveIndex(ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    makeMe.entityPersister.save(memoryTracker);
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
    assertThat(
        memoryTracker.getForgettingCurveIndex(),
        equalTo(ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX));
  }

  @Test
  void shouldStillCountAccidentalMatchTowardWrongAnswerThreshold()
      throws UnexpectedNoAccessRightException {
    var now = testabilitySettings.getCurrentUTCTimestamp();
    assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));

    for (int i = 0; i < 4; i++) {
      controller.answerSpelling(
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please(), answerDTO);
    }
    assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));

    controller.answerSpelling(
        makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please(), answerDTO);
    assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(true));
  }
}
