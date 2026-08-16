package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  @Nested
  class UniqueMatch {

    @Test
    void shouldWeakenUniqueMatchedSpellingTrackerWithoutRecallCredit()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(matchedSpellingTracker.getNextRecallAt());
      Timestamp dueBefore = matchedSpellingTracker.getNextRecallAt();
      Timestamp lastRecalledBefore = matchedSpellingTracker.getLastRecalledAt();
      Integer recallCountBefore = matchedSpellingTracker.getRecallCount();
      Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
      int wrongCountBefore =
          memoryTrackerService.countWrongAnswersInPeriod(matchedSpellingTracker, now, 14);

      controller.answerSpelling(recallPrompt, answerDTO);

      assertThat(matchedSpellingTracker.getStability(), equalTo(115f));
      assertThat(matchedSpellingTracker.getDifficulty(), nullValue());
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
    void shouldNotDropMatchedSpellingTrackerBelowStabilityFloor()
        throws UnexpectedNoAccessRightException {
      matchedSpellingTracker.setStability(ForgettingCurve.ASSIMILATE_STABILITY_HOURS);
      matchedSpellingTracker.setNextRecallAt(matchedSpellingTracker.calculateNextRecallAt());
      makeMe.entityPersister.save(matchedSpellingTracker);

      controller.answerSpelling(recallPrompt, answerDTO);

      assertThat(
          matchedSpellingTracker.getStability(),
          equalTo(ForgettingCurve.ASSIMILATE_STABILITY_HOURS));
    }

    @Test
    void shouldNotPostponeAlreadyDueMatchedSpellingTracker()
        throws UnexpectedNoAccessRightException {
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
      float stabilityBefore = understandingTracker.getStability();
      answerDTO = spellingAnswer(understandingNote.getTitle());

      controller.answerSpelling(recallPrompt, answerDTO);

      assertThat(understandingTracker.getStability(), lessThan(stabilityBefore));
      assertLinkedConfusionAdjustedTracker(understandingTracker);
    }

    @Test
    void shouldPreferActiveSpellingWhenUnderstandingAlsoExists()
        throws UnexpectedNoAccessRightException {
      MemoryTracker understandingTracker = ownedTracker(matchedSpellingTracker.getNote());
      float understandingStabilityBefore = understandingTracker.getStability();

      controller.answerSpelling(recallPrompt, answerDTO);

      assertLinkedConfusionAdjustedTracker(matchedSpellingTracker);
      assertThat(understandingTracker.getStability(), equalTo(understandingStabilityBefore));
    }

    @ParameterizedTest
    @CsvSource({"removedFromTracking", "deletedAt"})
    void shouldFallBackToUnderstandingWhenSpellingTrackerBecomesInactive(String inactivationMethod)
        throws UnexpectedNoAccessRightException {
      MemoryTracker understandingTracker = ownedTracker(matchedSpellingTracker.getNote());
      if ("removedFromTracking".equals(inactivationMethod)) {
        matchedSpellingTracker.setRemovedFromTracking(true);
      } else {
        matchedSpellingTracker.setDeletedAt(testabilitySettings.getCurrentUTCTimestamp());
      }
      makeMe.entityPersister.save(matchedSpellingTracker);

      controller.answerSpelling(recallPrompt, answerDTO);

      assertLinkedConfusionAdjustedTracker(understandingTracker);
    }
  }

  @Nested
  class Eligibility {

    @Test
    void shouldNotSelectPropertyTracker() throws UnexpectedNoAccessRightException {
      Note note = ownedNoteTitled("Property Match Title");
      MemoryTracker propertyTracker =
          makeMe
              .aMemoryTrackerFor(note)
              .stabilityAndNextRecallAt(200.0f)
              .propertyKey("topic")
              .please();
      assertIneligibleTrackerIsUnchangedAndUnlinked(note, propertyTracker);
    }

    @Test
    void shouldNotSelectCommissionedTracker() throws UnexpectedNoAccessRightException {
      Note note = ownedNoteTitled("Commissioned Match Title");
      MemoryTracker commissionedTracker =
          makeMe.aMemoryTrackerFor(note).stabilityAndNextRecallAt(200.0f).commissioned().please();
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
  }

  @Nested
  class Ambiguous {

    @Test
    void shouldLeaveAmbiguousMatchedTrackersUnchanged() throws UnexpectedNoAccessRightException {
      String sharedTitle = "Shared Accidental Title";
      MemoryTracker firstTracker = ownedSpellingTracker(ownedNoteTitled(sharedTitle));
      MemoryTracker secondTracker = ownedSpellingTracker(ownedNoteTitled(sharedTitle));
      float firstStabilityBefore = firstTracker.getStability();
      float secondStabilityBefore = secondTracker.getStability();
      Timestamp firstDueBefore = firstTracker.getNextRecallAt();
      Timestamp secondDueBefore = secondTracker.getNextRecallAt();
      answerDTO = spellingAnswer(sharedTitle);

      controller.answerSpelling(recallPrompt, answerDTO);

      assertThat(recallPrompt.getAnswer().getConfusionAdjustedMemoryTracker(), nullValue());
      assertThat(firstTracker.getStability(), equalTo(firstStabilityBefore));
      assertThat(secondTracker.getStability(), equalTo(secondStabilityBefore));
      assertThat(firstTracker.getNextRecallAt(), equalTo(firstDueBefore));
      assertThat(secondTracker.getNextRecallAt(), equalTo(secondDueBefore));
    }
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
    float stabilityBefore = tracker.getStability();
    answerDTO = spellingAnswer(note.getTitle());

    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(recallPrompt.getAnswer().getConfusionAdjustedMemoryTracker(), nullValue());
    assertThat(tracker.getStability(), equalTo(stabilityBefore));
  }
}
