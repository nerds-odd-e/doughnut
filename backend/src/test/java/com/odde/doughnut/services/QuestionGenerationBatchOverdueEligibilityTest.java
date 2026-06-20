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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchOverdueEligibilityTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchRepository batchRepository;

  User user;
  Note note;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
  }

  @Test
  void includesUserWhenOverdueAfterTargetTimePassed() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void includesUserWhenOverdueHoursAfterTargetTime() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 12, 15));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void includesUserWhoseTargetCrossesMidnightWhenOverdue() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 16, 0, 30));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 23, 45));
    createAnsweredRecall(recallTime);

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserWhoSubmittedAfterDueInstant() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0));
    createAnsweredRecall(recallTime);
    saveSubmittedBatchAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 0)));

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
    makeMe.entityPersister.flush();
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
}
