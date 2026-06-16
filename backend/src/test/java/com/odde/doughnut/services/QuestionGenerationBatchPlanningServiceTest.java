package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
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
}
