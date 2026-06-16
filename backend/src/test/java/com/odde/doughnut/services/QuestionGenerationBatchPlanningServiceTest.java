package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchPlanningServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchUserStateRepository userStateRepository;

  User user;
  Timestamp currentTime;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
  }

  @Test
  void userIsPastSubmissionGateWhenNoSuccessfulSubmissionExists() {
    assertThat(planningService.isUserPastSubmissionGate(user, currentTime), is(true));
  }

  @Nested
  class WithLastSuccessfulSubmission {
    Timestamp lastSuccessfulSubmission;

    @BeforeEach
    void setup() {
      lastSuccessfulSubmission = makeMe.aTimestamp().of(9, 8).fromShanghai().please();
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(lastSuccessfulSubmission);
      userStateRepository.save(state);
      makeMe.entityPersister.flush();
    }

    @Test
    void userIsNotPastSubmissionGateWhenLastSubmissionIs22Hours59MinutesOld() {
      Timestamp at =
          new Timestamp(
              lastSuccessfulSubmission.getTime()
                  + TimeUnit.HOURS.toMillis(22)
                  + TimeUnit.MINUTES.toMillis(59));

      assertThat(planningService.isUserPastSubmissionGate(user, at), is(false));
    }

    @Test
    void userIsPastSubmissionGateWhenLastSubmissionIsExactly23HoursOld() {
      Timestamp at =
          new Timestamp(lastSuccessfulSubmission.getTime() + TimeUnit.HOURS.toMillis(23));

      assertThat(planningService.isUserPastSubmissionGate(user, at), is(true));
    }

    @Test
    void userIsPastSubmissionGateWhenLastSubmissionIsJustOver23HoursOld() {
      Timestamp at =
          new Timestamp(
              lastSuccessfulSubmission.getTime()
                  + TimeUnit.HOURS.toMillis(23)
                  + TimeUnit.MINUTES.toMillis(1));

      assertThat(planningService.isUserPastSubmissionGate(user, at), is(true));
    }
  }

  @Nested
  class FindUsersEligibleForBatchSubmission {
    Note note;
    MemoryTracker memoryTracker;

    @BeforeEach
    void setupRecallFixtures() {
      note = makeMe.aNote().notebookOwnedBy(user).please();
      memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    }

    @Test
    void excludesUserWithNoRecentRecallActivity() {
      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(currentTime);

      assertThat(candidates, empty());
    }

    @Test
    void includesUserWithOneRecentRecallAndNoSuccessfulSubmission() {
      Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(oneHourAgo)
          .please();

      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(currentTime);

      assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
    }

    @Test
    void excludesUserWhoseOnlyRecallIsOlderThanSevenDays() {
      Timestamp eightDaysAgo = new Timestamp(currentTime.getTime() - TimeUnit.DAYS.toMillis(8));
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(eightDaysAgo)
          .please();

      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(currentTime);

      assertThat(candidates, empty());
    }

    @Test
    void excludesUserWithRecentRecallButRecentSuccessfulSubmission() {
      Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(oneHourAgo)
          .please();
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(oneHourAgo);
      userStateRepository.save(state);
      makeMe.entityPersister.flush();

      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(currentTime);

      assertThat(candidates, empty());
    }
  }

  @Nested
  class FindUsersDueInCurrentCronHour {
    Note note;
    MemoryTracker memoryTracker;

    @BeforeEach
    void setupRecallFixtures() {
      note = makeMe.aNote().notebookOwnedBy(user).please();
      memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    }

    @Test
    void includesUserWhoseTargetFallsInCurrentCronHour() {
      Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30));
      Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(recallTime)
          .please();

      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

      assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
    }

    @Test
    void excludesUserWhoseTargetFallsInAdjacentCronHour() {
      Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 11, 15));
      Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(recallTime)
          .please();

      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

      assertThat(candidates, empty());
    }

    @Test
    void includesUserWhoseTargetCrossesMidnightWhenCronHourMatches() {
      Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 16, 0, 30));
      Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 23, 45));
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(recallTime)
          .please();

      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

      assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
    }

    @Test
    void excludesUserPastSubmissionGateEvenWhenDueInCurrentCronHour() {
      Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30));
      Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(recallTime)
          .please();
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(
          new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(1)));
      userStateRepository.save(state);
      makeMe.entityPersister.flush();

      List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

      assertThat(candidates, empty());
    }
  }

  @Nested
  class FindCandidateMemoryTrackersForBatchGeneration {
    Note note;
    MemoryTracker dueTracker;
    Timestamp dueBy;

    @BeforeEach
    void setupTrackerFixtures() {
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
}
