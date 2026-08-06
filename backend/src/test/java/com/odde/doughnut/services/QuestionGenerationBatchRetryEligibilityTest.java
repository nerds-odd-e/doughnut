package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchRetryEligibilityTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;

  User user;
  Note note;
  MemoryTracker memoryTracker;
  Timestamp cronTime;
  Timestamp recallTime;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).please();
    cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 11, 15));
    recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(recallTime)
        .please();
  }

  @ParameterizedTest
  @EnumSource(
      value = QuestionGenerationBatchStatus.class,
      names = {"FAILED", "EXPIRED"})
  void includesUserWithOpenAiTerminalBatchWhenSubmittedSinceDueInstant(
      QuestionGenerationBatchStatus terminalStatus) {
    makeMe.aQuestionGenerationBatch().forUser(user).completedAt(hoursBefore(cronTime, 1)).please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(terminalStatus)
        .openaiBatchId("batch-" + terminalStatus.name().toLowerCase())
        .plannedAt(hoursBefore(cronTime, 2))
        .please();
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserWhoSubmittedSinceLastDueInstantBeforeTodaysTarget() {
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .completedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0)))
        .please();
    makeMe.entityPersister.flush();

    assertThat(
        planningService.findUsersEligibleForBatchSubmission(
            Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 0))),
        empty());
  }

  @Test
  void excludesRetryEligibleUserWhileSubmittedBatchIsInFlight() {
    makeMe.aQuestionGenerationBatch().forUser(user).completedAt(hoursBefore(cronTime, 1)).please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(QuestionGenerationBatchStatus.FAILED)
        .openaiBatchId("batch-failed")
        .plannedAt(hoursBefore(cronTime, 2))
        .please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .submittedInFlight(new Timestamp(cronTime.getTime() - TimeUnit.MINUTES.toMillis(30)))
        .openaiBatchId("batch-in-flight")
        .please();
    makeMe.entityPersister.flush();

    assertThat(planningService.findUsersEligibleForBatchSubmission(cronTime), empty());
  }

  @Test
  void excludesRecentlySuccessfulUserWithoutOpenAiFailureRetryBatch() {
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .completedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 45)))
        .please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(QuestionGenerationBatchStatus.FAILED)
        .plannedAt(hoursBefore(cronTime, 2))
        .please();
    makeMe.entityPersister.flush();

    assertThat(planningService.findUsersEligibleForBatchSubmission(cronTime), empty());
  }

  private static Timestamp hoursBefore(Timestamp base, int hours) {
    return new Timestamp(base.getTime() - TimeUnit.HOURS.toMillis(hours));
  }
}
