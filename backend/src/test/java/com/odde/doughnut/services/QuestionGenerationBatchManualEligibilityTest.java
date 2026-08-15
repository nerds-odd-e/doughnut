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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchManualEligibilityTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;

  User user;
  Timestamp currentTime;
  Note note;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).please();
  }

  @Test
  void includesRecentRecallUserBeforeTodaysTargetWhenAlreadySubmittedSinceLastDueInstant() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 0));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .completedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0)))
        .please();
    makeMe.entityPersister.flush();

    List<User> scheduledCandidates = planningService.findUsersEligibleForBatchSubmission(cronTime);
    List<User> manualCandidates =
        planningService.findUsersEligibleForManualBatchSubmission(cronTime);

    assertThat(scheduledCandidates, empty());
    assertThat(manualCandidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserWithNoRecentRecallActivity() {
    assertThat(planningService.findUsersEligibleForManualBatchSubmission(currentTime), empty());
  }

  @Test
  void includesUserWithPriorSubmittedBatchRegardlessOfOverdueRule() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    makeMe.aQuestionGenerationBatch().forUser(user).completedAt(oneHourAgo).please();
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForManualBatchSubmission(currentTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserWithSubmittedBatchInFlight() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    makeMe.aQuestionGenerationBatch().forUser(user).submittedInFlight(oneHourAgo).please();
    makeMe.entityPersister.flush();

    assertThat(planningService.findUsersEligibleForManualBatchSubmission(currentTime), empty());
  }

  @Test
  void includesUserWithOpenAiFailureBatchWhenNoSubmittedBatchInFlight() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    makeMe.aQuestionGenerationBatch().forUser(user).completedAt(oneHourAgo).please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(QuestionGenerationBatchStatus.FAILED)
        .openaiBatchId("batch-failed")
        .plannedAt(oneHourAgo)
        .please();
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForManualBatchSubmission(currentTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  private void createAnsweredRecall(Timestamp answerTime) {
    makeMe
        .aRecallPrompt()
        .withMcqForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(answerTime)
        .please();
  }
}
