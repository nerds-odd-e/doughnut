package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchCandidateMemoryTrackersTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;

  User user;
  Timestamp currentTime;
  Note note;
  MemoryTracker dueTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    dueTracker = makeMe.aMemoryTrackerFor(note).nextRecallAt(hoursFrom(currentTime, 24)).please();
  }

  @Test
  void includesActiveNonSpellingTrackerDueWithin48Hours() {
    assertThat(candidateIds(), contains(dueTracker.getId()));
  }

  @Test
  void excludesTrackerDueAfter48Hours() {
    MemoryTracker notDueTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .nextRecallAt(hoursFrom(currentTime, 49))
            .please();

    assertThat(candidateIds(), not(hasItem(notDueTracker.getId())));
  }

  @Test
  void excludesRemovedTracker() {
    MemoryTracker removedTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .removedFromTracking()
            .nextRecallAt(hoursFrom(currentTime, 1))
            .please();

    assertThat(candidateIds(), not(hasItem(removedTracker.getId())));
  }

  @Test
  void excludesDeletedTracker() {
    MemoryTracker deletedTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .nextRecallAt(hoursFrom(currentTime, 1))
            .deletedAt(currentTime)
            .please();

    assertThat(candidateIds(), not(hasItem(deletedTracker.getId())));
  }

  @Test
  void excludesSpellingTracker() {
    MemoryTracker spellingTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .spelling()
            .nextRecallAt(hoursFrom(currentTime, 1))
            .please();

    assertThat(candidateIds(), not(hasItem(spellingTracker.getId())));
  }

  @Test
  void excludesCommissionedTracker() {
    MemoryTracker commissionedTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .commissioned()
            .nextRecallAt(hoursFrom(currentTime, 1))
            .please();

    assertThat(candidateIds(), not(hasItem(commissionedTracker.getId())));
  }

  @Test
  void includesTrackerWithAnsweredPrompt() {
    makeMe
        .aRecallPrompt()
        .withMcqForNote(note)
        .forMemoryTracker(dueTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(currentTime)
        .please();

    assertThat(candidateIds(), contains(dueTracker.getId()));
  }

  @Test
  void includesTrackerWithUnansweredContestedPrompt() {
    makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(dueTracker).contested().please();

    assertThat(candidateIds(), contains(dueTracker.getId()));
  }

  @Test
  void excludesTrackerWithUnansweredNonContestedPrompt() {
    makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(dueTracker).please();

    assertThat(candidates(), empty());
  }

  @Test
  void includesPropertyTrackerDueWithin48Hours() {
    MemoryTracker propertyTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .propertyKey("topic")
            .nextRecallAt(hoursFrom(currentTime, 12))
            .please();

    assertThat(candidateIds(), hasItem(propertyTracker.getId()));
  }

  private List<MemoryTracker> candidates() {
    return planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);
  }

  private List<Integer> candidateIds() {
    return candidates().stream().map(MemoryTracker::getId).toList();
  }

  private static Timestamp hoursFrom(Timestamp base, int hours) {
    return new Timestamp(base.getTime() + TimeUnit.HOURS.toMillis(hours));
  }
}
