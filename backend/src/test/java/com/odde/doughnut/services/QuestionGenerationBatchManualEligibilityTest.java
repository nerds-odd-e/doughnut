package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
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
  @Autowired QuestionGenerationBatchUserStateRepository userStateRepository;
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
  void includesRecentRecallUserOutsideSilentPeriodTargetHour() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 11, 15));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);

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
  void excludesUserWithRecentSuccessfulSubmissionStillInsideGate() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    saveRecentSuccessfulSubmission(oneHourAgo);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForManualBatchSubmission(currentTime);

    assertThat(candidates, empty());
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
  void includesUserInsideGateWhenOpenAiFailureRetryPolicyAllowsIt() {
    Timestamp oneHourAgo = new Timestamp(currentTime.getTime() - TimeUnit.HOURS.toMillis(1));
    createAnsweredRecall(oneHourAgo);
    saveRecentSuccessfulSubmission(oneHourAgo);
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

  private void saveRecentSuccessfulSubmission(Timestamp submittedAt) {
    QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
    state.setUser(user);
    state.setLastSuccessfulSubmittedAt(submittedAt);
    userStateRepository.save(state);
  }
}
