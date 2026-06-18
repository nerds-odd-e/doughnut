package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
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
  Timestamp dueBy;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    dueTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();
    dueBy = new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(48));
  }

  @Test
  void includesActiveNonSpellingTrackerDueWithin48Hours() {
    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(), contains(dueTracker.getId()));
  }

  @Test
  void excludesTrackerDueAfter48Hours() {
    MemoryTracker notDueTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .by(user)
            .nextRecallAt(new Timestamp(dueBy.getTime() + TimeUnit.HOURS.toMillis(1)))
            .please();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(), contains(dueTracker.getId()));
    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(),
        not(contains(notDueTracker.getId())));
  }

  @Test
  void excludesRemovedTracker() {
    MemoryTracker removedTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .by(user)
            .removedFromTracking()
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(1)))
            .please();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(),
        not(contains(removedTracker.getId())));
  }

  @Test
  void excludesDeletedTracker() {
    MemoryTracker deletedTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(1)))
            .please();
    deletedTracker.setDeletedAt(currentTime);
    makeMe.entityPersister.flush();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(),
        not(contains(deletedTracker.getId())));
  }

  @Test
  void excludesSpellingTracker() {
    MemoryTracker spellingTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .by(user)
            .spelling()
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(1)))
            .please();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(),
        not(contains(spellingTracker.getId())));
  }

  @Test
  void includesTrackerWithAnsweredPrompt() {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(dueTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(currentTime)
        .please();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(), contains(dueTracker.getId()));
  }

  @Test
  void includesTrackerWithUnansweredContestedPrompt() {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(dueTracker)
        .contested()
        .please();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(), contains(dueTracker.getId()));
  }

  @Test
  void excludesTrackerWithUnansweredNonContestedPrompt() {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(dueTracker)
        .please();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(candidates, empty());
  }

  @Test
  void includesPropertyTrackerDueWithin48Hours() {
    MemoryTracker propertyTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .propertyKey("topic")
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(12)))
            .please();

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(),
        containsInAnyOrder(dueTracker.getId(), propertyTracker.getId()));
  }
}
