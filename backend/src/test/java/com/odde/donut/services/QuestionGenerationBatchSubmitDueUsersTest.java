package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.QuestionGenerationBatchSubmissionSummaryDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.testability.CommittedUserCleanup;
import com.odde.donut.testability.SpringTestBase;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Isolated
class QuestionGenerationBatchSubmitDueUsersTest extends SpringTestBase {

  static final String COMMITTED_USER_PREFIX = "batch-due-";

  @Autowired QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired EntityManager entityManager;
  @Autowired PlatformTransactionManager transactionManager;

  final Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 8, 3, 16, 45));

  @BeforeEach
  void cleanupStaleCommittedFixtures() {
    inCommittedTransaction(this::deleteCommittedDueUserFixtures);
  }

  @AfterEach
  void cleanupCommittedState() {
    inCommittedTransaction(this::deleteCommittedDueUserFixtures);
  }

  @Test
  void skipsDueUserWithNoCandidateTrackersWithoutSubmittedBatch() {
    User[] dueUser = new User[1];
    inCommittedTransaction(
        () -> {
          dueUser[0] = makeMe.aUser(COMMITTED_USER_PREFIX + UUID.randomUUID()).please();

          Note note = makeMe.aNote().notebookOwnedBy(dueUser[0]).please();
          var tracker =
              makeMe
                  .aMemoryTrackerFor(note)
                  .nextRecallAt(new Timestamp(cronTime.getTime() + TimeUnit.HOURS.toMillis(24)))
                  .please();
          Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 8, 3, 15, 45));
          makeMe
              .aRecallPrompt()
              .withMcqForNote(note)
              .forMemoryTracker(tracker)
              .answerChoiceIndex(0)
              .answerTimestamp(recallTime)
              .please();
          makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(tracker).please();
        });

    QuestionGenerationBatchSubmissionSummaryDTO[] summary =
        new QuestionGenerationBatchSubmissionSummaryDTO[1];
    inCommittedTransaction(() -> summary[0] = submitDueUsersService.submitDueUsers(cronTime));

    inCommittedTransaction(
        () -> {
          assertThat(summary[0].getConsideredUserCount(), equalTo(1));
          assertThat(summary[0].getSubmittedCount(), equalTo(0));
          assertThat(summary[0].getFailedCount(), equalTo(0));
          assertThat(summary[0].getSkippedCount(), equalTo(1));

          assertThat(
              batchRepository.findAll().stream()
                  .anyMatch(batch -> batch.getUser().getId().equals(dueUser[0].getId())),
              is(false));
          assertThat(
              batchRepository.findLatestSubmittedAtByUser_Id(dueUser[0].getId()).isPresent(),
              is(false));
        });
  }

  private void inCommittedTransaction(Runnable action) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    template.executeWithoutResult(status -> action.run());
  }

  private void deleteCommittedDueUserFixtures() {
    CommittedUserCleanup.deleteByUserExternalIdentifierLike(
        entityManager, COMMITTED_USER_PREFIX + "%");
  }
}
