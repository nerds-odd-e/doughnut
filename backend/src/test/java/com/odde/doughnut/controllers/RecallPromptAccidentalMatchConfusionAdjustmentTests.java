package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.entities.Fsrs;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptAccidentalMatchConfusionAdjustmentTests extends RecallPromptControllerTestBase {

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
    void accidentalMatchLeavesAConfusionRecallLogOnTheMatchedTrackerLinkedToTheSameAnswer()
        throws UnexpectedNoAccessRightException {
      controller.answerSpelling(recallPrompt, answerDTO);

      Integer promptedAnswerId =
          memoryTrackerController.getRecallLogs(promptedTracker).get(0).getAnswerId();
      List<RecallLog> matchedLogs = memoryTrackerController.getRecallLogs(matchedSpellingTracker);
      assertThat(matchedLogs.get(0).isConfusion(), is(true));
      assertThat(matchedLogs.get(0).getAnswerId(), equalTo(promptedAnswerId));
    }

    @Test
    void shouldWeakenUniqueMatchedSpellingTrackerWithoutRecallCredit()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(matchedSpellingTracker.getNextRecallAt());
      Timestamp dueBefore = matchedSpellingTracker.getNextRecallAt();
      Timestamp lastRecalledBefore = matchedSpellingTracker.getLastRecalledAt();
      Integer recallCountBefore = matchedSpellingTracker.getRecallCount();
      int wrongCountBefore =
          memoryTrackerController.getThresholdExceeded(matchedSpellingTracker).wrongCount();

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
          memoryTrackerController.getThresholdExceeded(matchedSpellingTracker).wrongCount(),
          equalTo(wrongCountBefore));
    }

    @Test
    void shouldNotDropMatchedSpellingTrackerBelowStabilityFloor()
        throws UnexpectedNoAccessRightException {
      matchedSpellingTracker.setStability(Fsrs.NEW_STABILITY_HOURS);
      matchedSpellingTracker.setNextRecallAt(matchedSpellingTracker.calculateNextRecallAt());
      makeMe.entityPersister.save(matchedSpellingTracker);

      controller.answerSpelling(recallPrompt, answerDTO);

      assertThat(matchedSpellingTracker.getStability(), equalTo(Fsrs.NEW_STABILITY_HOURS));
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
      assertConfusionLogged(understandingTracker);
    }

    @Test
    void shouldPreferActiveSpellingWhenUnderstandingAlsoExists()
        throws UnexpectedNoAccessRightException {
      MemoryTracker understandingTracker = ownedTracker(matchedSpellingTracker.getNote());
      float understandingStabilityBefore = understandingTracker.getStability();

      controller.answerSpelling(recallPrompt, answerDTO);

      assertConfusionLogged(matchedSpellingTracker);
      assertThat(understandingTracker.getStability(), equalTo(understandingStabilityBefore));
      assertNoConfusionLog(understandingTracker);
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

      assertConfusionLogged(understandingTracker);
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
      assertIneligibleTrackerIsUnchanged(note, propertyTracker);
    }

    @Test
    void shouldNotSelectCommissionedTracker() throws UnexpectedNoAccessRightException {
      Note note = ownedNoteTitled("Commissioned Match Title");
      MemoryTracker commissionedTracker =
          makeMe.aMemoryTrackerFor(note).stabilityAndNextRecallAt(200.0f).commissioned().please();
      assertIneligibleTrackerIsUnchanged(note, commissionedTracker);
    }

    @Test
    void shouldNotCreateOrLinkTrackerWhenMatchedNoteHasNoneEligible()
        throws UnexpectedNoAccessRightException {
      Note note = ownedNoteTitled("Bare Match Title");
      answerDTO = spellingAnswer(note.getTitle());

      controller.answerSpelling(recallPrompt, answerDTO);

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

      assertNoConfusionLog(firstTracker);
      assertNoConfusionLog(secondTracker);
      assertThat(firstTracker.getStability(), equalTo(firstStabilityBefore));
      assertThat(secondTracker.getStability(), equalTo(secondStabilityBefore));
      assertThat(firstTracker.getNextRecallAt(), equalTo(firstDueBefore));
      assertThat(secondTracker.getNextRecallAt(), equalTo(secondDueBefore));
    }
  }

  private Note ownedNoteTitled(String title) {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(title).please();
  }

  private void assertConfusionLogged(MemoryTracker tracker)
      throws UnexpectedNoAccessRightException {
    assertThat(memoryTrackerController.getRecallLogs(tracker).get(0).isConfusion(), is(true));
  }

  private void assertNoConfusionLog(MemoryTracker tracker) throws UnexpectedNoAccessRightException {
    assertThat(memoryTrackerController.getRecallLogs(tracker), hasSize(0));
  }

  private void assertIneligibleTrackerIsUnchanged(Note note, MemoryTracker tracker)
      throws UnexpectedNoAccessRightException {
    float stabilityBefore = tracker.getStability();
    answerDTO = spellingAnswer(note.getTitle());

    controller.answerSpelling(recallPrompt, answerDTO);

    assertNoConfusionLog(tracker);
    assertThat(tracker.getStability(), equalTo(stabilityBefore));
  }
}
