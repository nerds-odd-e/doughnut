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
class QuestionGenerationBatchRetryEligibilityTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
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

  @Test
  void includesUserWithFailedOpenAiBatchWhenSubmittedSinceDueInstant() {
    assertUserEligibleForOpenAiFailureRetryWhenSubmittedSinceDueInstant(
        QuestionGenerationBatchStatus.FAILED);
  }

  @Test
  void includesUserWithExpiredOpenAiBatchWhenSubmittedSinceDueInstant() {
    assertUserEligibleForOpenAiFailureRetryWhenSubmittedSinceDueInstant(
        QuestionGenerationBatchStatus.EXPIRED);
  }

  private void assertUserEligibleForOpenAiFailureRetryWhenSubmittedSinceDueInstant(
      QuestionGenerationBatchStatus terminalStatus) {
    saveSubmittedBatchAt(new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(1)));
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
  void excludesUserWhoSubmittedSinceLastDueInstantBeforeTodaysTarget() {
    saveSubmittedBatchAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0)));
    makeMe.entityPersister.flush();

    List<User> candidates =
        planningService.findUsersEligibleForBatchSubmission(
            Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 0)));

    assertThat(candidates, empty());
  }

  @Test
  void excludesRetryEligibleUserWhileSubmittedBatchIsInFlight() {
    saveSubmittedBatchAt(new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(1)));
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
    saveSubmittedBatchAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 45)));
    QuestionGenerationBatch failedBatch = new QuestionGenerationBatch();
    failedBatch.setUser(user);
    failedBatch.setStatus(QuestionGenerationBatchStatus.FAILED);
    failedBatch.setPlannedAt(new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(2)));
    batchRepository.save(failedBatch);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates, empty());
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
