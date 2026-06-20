package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
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
  @Autowired QuestionGenerationBatchRepository batchRepository;

  User user;
  Timestamp currentTime;
  Note note;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
  }

  @Test
  void includesRecentRecallUserBeforeTodaysTargetWhenAlreadySubmittedSinceLastDueInstant() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 0));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);
    saveSubmittedBatchAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0)));
    makeMe.entityPersister.flush();

    List<User> scheduledCandidates = planningService.findUsersEligibleForBatchSubmission(cronTime);
    List<User> manualCandidates =
        planningService.findUsersEligibleForManualBatchSubmission(cronTime);

    assertThat(scheduledCandidates, empty());
    assertThat(manualCandidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserWithNoRecentRecallActivity() {
    List<User> candidates = planningService.findUsersEligibleForManualBatchSubmission(currentTime);

    assertThat(candidates, empty());
  }

  @Test
  void includesUserWithPriorSubmittedBatchRegardlessOfOverdueRule() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    saveSubmittedBatchAt(oneHourAgo);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForManualBatchSubmission(currentTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserWithSubmittedBatchInFlight() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
    batch.setPlannedAt(oneHourAgo);
    batchRepository.save(batch);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForManualBatchSubmission(currentTime);

    assertThat(candidates, empty());
  }

  @Test
  void includesUserWithOpenAiFailureBatchWhenNoSubmittedBatchInFlight() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    saveSubmittedBatchAt(oneHourAgo);
    QuestionGenerationBatch failedBatch = new QuestionGenerationBatch();
    failedBatch.setUser(user);
    failedBatch.setStatus(QuestionGenerationBatchStatus.FAILED);
    failedBatch.setOpenaiBatchId("batch-failed");
    failedBatch.setPlannedAt(oneHourAgo);
    batchRepository.save(failedBatch);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForManualBatchSubmission(currentTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  private void createAnsweredRecall(Timestamp answerTime) {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(answerTime)
        .please();
  }

  private void saveSubmittedBatchAt(Timestamp submittedAt) {
    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    batch.setPlannedAt(submittedAt);
    batch.setSubmittedAt(submittedAt);
    batchRepository.save(batch);
  }
}
