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
  @Autowired QuestionGenerationBatchUserStateRepository userStateRepository;
  @Autowired QuestionGenerationBatchRepository batchRepository;

  User user;
  Note note;
  MemoryTracker memoryTracker;
  Timestamp cronTime;
  Timestamp recallTime;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
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
  void includesUserWithOpenAiFailureBatchOutsideTargetCronHour(
      QuestionGenerationBatchStatus terminalStatus) {
    saveRecentSuccessfulSubmission();
    QuestionGenerationBatch failedBatch = new QuestionGenerationBatch();
    failedBatch.setUser(user);
    failedBatch.setStatus(terminalStatus);
    failedBatch.setOpenaiBatchId("batch-" + terminalStatus.name().toLowerCase());
    failedBatch.setPlannedAt(new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(2)));
    batchRepository.save(failedBatch);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesFirstTimeUserOutsideTargetCronHour() {
    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates, empty());
  }

  @Test
  void excludesRetryEligibleUserWhileSubmittedBatchIsInFlight() {
    saveRecentSuccessfulSubmission();
    QuestionGenerationBatch failedBatch = new QuestionGenerationBatch();
    failedBatch.setUser(user);
    failedBatch.setStatus(QuestionGenerationBatchStatus.FAILED);
    failedBatch.setOpenaiBatchId("batch-failed");
    failedBatch.setPlannedAt(new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(2)));
    batchRepository.save(failedBatch);
    QuestionGenerationBatch submittedBatch = new QuestionGenerationBatch();
    submittedBatch.setUser(user);
    submittedBatch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
    submittedBatch.setOpenaiBatchId("batch-in-flight");
    submittedBatch.setPlannedAt(new Timestamp(cronTime.getTime() - TimeUnit.MINUTES.toMillis(30)));
    batchRepository.save(submittedBatch);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates, empty());
  }

  @Test
  void excludesRecentlySuccessfulUserWithoutOpenAiFailureRetryBatch() {
    saveRecentSuccessfulSubmission();
    QuestionGenerationBatch failedBatch = new QuestionGenerationBatch();
    failedBatch.setUser(user);
    failedBatch.setStatus(QuestionGenerationBatchStatus.FAILED);
    failedBatch.setPlannedAt(new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(2)));
    batchRepository.save(failedBatch);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates, empty());
  }

  private void saveRecentSuccessfulSubmission() {
    QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
    state.setUser(user);
    state.setLastSuccessfulSubmittedAt(
        new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(1)));
    userStateRepository.save(state);
  }
}
