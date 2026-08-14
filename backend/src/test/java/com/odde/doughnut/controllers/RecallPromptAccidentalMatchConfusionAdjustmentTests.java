package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptAccidentalMatchConfusionAdjustmentTests extends RecallPromptControllerTestBase {

  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  MemoryTracker promptedTracker;
  MemoryTracker matchedSpellingTracker;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO;

  @BeforeEach
  void setup() {
    promptedTracker = ownedSpellingTracker(ownedNote());
    recallPrompt = spellingPrompt(promptedTracker);
    Note matchedNote = ownedNoteTitled("Another Note Title");
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
    assertLinkedConfusionAdjustedTracker(matchedSpellingTracker);
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

  @Test
  void shouldWeakenUniqueMatchedUnderstandingTrackerWhenSpellingIsAbsent()
      throws UnexpectedNoAccessRightException {
    Note understandingNote = ownedNoteTitled("Understanding Match Title");
    MemoryTracker understandingTracker = ownedTracker(understandingNote);
    answerDTO = spellingAnswer(understandingNote.getTitle());
    float strengthBefore = understandingTracker.getForgettingCurveIndex();

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(
        understandingTracker.getForgettingCurveIndex(),
        equalTo(strengthBefore - ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT));
    assertLinkedConfusionAdjustedTracker(understandingTracker);
  }

  @Test
  void shouldPreferActiveSpellingWhenUnderstandingAlsoExists()
      throws UnexpectedNoAccessRightException {
    MemoryTracker understandingTracker = ownedTracker(matchedSpellingTracker.getNote());
    float understandingStrengthBefore = understandingTracker.getForgettingCurveIndex();

    controller.answerSpelling(recallPrompt, answerDTO);

    assertLinkedConfusionAdjustedTracker(matchedSpellingTracker);
    assertThat(
        understandingTracker.getForgettingCurveIndex(), equalTo(understandingStrengthBefore));
  }

  @Test
  void shouldFallBackToUnderstandingWhenSpellingIsRemovedFromRecall()
      throws UnexpectedNoAccessRightException {
    MemoryTracker understandingTracker = ownedTracker(matchedSpellingTracker.getNote());
    matchedSpellingTracker.setRemovedFromTracking(true);
    makeMe.entityPersister.save(matchedSpellingTracker);

    controller.answerSpelling(recallPrompt, answerDTO);

    assertLinkedConfusionAdjustedTracker(understandingTracker);
  }

  @Test
  void shouldFallBackToUnderstandingWhenSpellingIsDeleted()
      throws UnexpectedNoAccessRightException {
    MemoryTracker understandingTracker = ownedTracker(matchedSpellingTracker.getNote());
    matchedSpellingTracker.setDeletedAt(testabilitySettings.getCurrentUTCTimestamp());
    makeMe.entityPersister.save(matchedSpellingTracker);

    controller.answerSpelling(recallPrompt, answerDTO);

    assertLinkedConfusionAdjustedTracker(understandingTracker);
  }

  @Test
  void shouldNotSelectPropertyTracker() throws UnexpectedNoAccessRightException {
    Note note = ownedNoteTitled("Property Match Title");
    MemoryTracker propertyTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .forgettingCurveAndNextRecallAt(200.0f)
            .propertyKey("topic")
            .please();
    assertIneligibleTrackerIsUnchangedAndUnlinked(note, propertyTracker);
  }

  @Test
  void shouldNotSelectCommissionedTracker() throws UnexpectedNoAccessRightException {
    Note note = ownedNoteTitled("Commissioned Match Title");
    MemoryTracker commissionedTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .forgettingCurveAndNextRecallAt(200.0f)
            .commissioned()
            .please();
    assertIneligibleTrackerIsUnchangedAndUnlinked(note, commissionedTracker);
  }

  @Test
  void shouldNotCreateOrLinkTrackerWhenMatchedNoteHasNoneEligible()
      throws UnexpectedNoAccessRightException {
    Note note = ownedNoteTitled("Bare Match Title");
    answerDTO = spellingAnswer(note.getTitle());

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(recallPrompt.getAnswer().getConfusionAdjustedMemoryTracker(), nullValue());
    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        hasSize(0));
  }

  @Test
  void shouldLeaveAmbiguousMatchedTrackersUnchanged() throws UnexpectedNoAccessRightException {
    String sharedTitle = "Shared Accidental Title";
    MemoryTracker firstTracker = ownedSpellingTracker(ownedNoteTitled(sharedTitle));
    MemoryTracker secondTracker = ownedSpellingTracker(ownedNoteTitled(sharedTitle));
    float firstStrengthBefore = firstTracker.getForgettingCurveIndex();
    float secondStrengthBefore = secondTracker.getForgettingCurveIndex();
    Timestamp firstDueBefore = firstTracker.getNextRecallAt();
    Timestamp secondDueBefore = secondTracker.getNextRecallAt();
    answerDTO = spellingAnswer(sharedTitle);

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(recallPrompt.getAnswer().getConfusionAdjustedMemoryTracker(), nullValue());
    assertThat(firstTracker.getForgettingCurveIndex(), equalTo(firstStrengthBefore));
    assertThat(secondTracker.getForgettingCurveIndex(), equalTo(secondStrengthBefore));
    assertThat(firstTracker.getNextRecallAt(), equalTo(firstDueBefore));
    assertThat(secondTracker.getNextRecallAt(), equalTo(secondDueBefore));
  }

  private Note ownedNoteTitled(String title) {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(title).please();
  }

  private void assertLinkedConfusionAdjustedTracker(MemoryTracker tracker) {
    assertThat(
        recallPrompt.getAnswer().getConfusionAdjustedMemoryTracker().getId(),
        equalTo(tracker.getId()));
  }

  private void assertIneligibleTrackerIsUnchangedAndUnlinked(Note note, MemoryTracker tracker)
      throws UnexpectedNoAccessRightException {
    float strengthBefore = tracker.getForgettingCurveIndex();
    answerDTO = spellingAnswer(note.getTitle());

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(recallPrompt.getAnswer().getConfusionAdjustedMemoryTracker(), nullValue());
    assertThat(tracker.getForgettingCurveIndex(), equalTo(strengthBefore));
  }
}
