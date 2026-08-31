package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionGenerationBatchRetentionWithoutTransactionTest {

  private static final String FIXTURE_PREFIX = "batch-prune-committed-";

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRetentionService retentionService;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @BeforeEach
  void cleanupStaleCommittedFixtures() {
    deleteCommittedPruneFixtures();
  }

  @AfterEach
  void cleanupCommittedState() {
    deleteCommittedPruneFixtures();
  }

  @Test
  void prunesOldFailedBatchWhenCalledWithoutASurroundingTransaction() {
    Timestamp currentTime = makeMe.aTimestamp().please();
    Integer batchId =
        inCommittedTransaction(
            () -> {
              User user = new User();
              String identifier = FIXTURE_PREFIX + UUID.randomUUID();
              user.setExternalIdentifier(identifier);
              user.setName(identifier);
              makeMe.entityPersister.save(user);
              Note note = makeMe.aNote().notebookOwnedBy(user).please();
              MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).please();
              QuestionGenerationBatch batch =
                  makeMe
                      .aQuestionGenerationBatch()
                      .forUser(user)
                      .status(QuestionGenerationBatchStatus.FAILED)
                      .plannedAt(oldTimestamp(currentTime))
                      .submittedAt(oldTimestamp(currentTime))
                      .openaiBatchId("batch-failed-notx")
                      .please();
              makeMe.entityPersister.flush();
              makeMe
                  .aQuestionGenerationBatchRequest()
                  .batch(batch)
                  .memoryTracker(tracker)
                  .status(QuestionGenerationBatchRequestStatus.IMPORTED)
                  .please();
              return batch.getId();
            });

    retentionService.pruneTerminalBatches(currentTime);

    inCommittedTransaction(
        () -> assertThat(batchRepository.findById(batchId).orElse(null), is(nullValue())));
  }

  private Timestamp oldTimestamp(Timestamp currentTime) {
    return new Timestamp(
        currentTime.getTime()
            - QuestionGenerationBatchRetentionService.DEFAULT_RETENTION_WINDOW.toMillis()
            - TimeUnit.DAYS.toMillis(1));
  }

  private void deleteCommittedPruneFixtures() {
    inCommittedTransaction(
        () ->
            QuestionGenerationBatchCommittedUserCleanup.deleteByUserExternalIdentifierLike(
                entityManager, FIXTURE_PREFIX + "%"));
  }

  private <T> T inCommittedTransaction(java.util.function.Supplier<T> action) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template.execute(status -> action.get());
  }

  private void inCommittedTransaction(Runnable action) {
    inCommittedTransaction(
        () -> {
          action.run();
          return null;
        });
  }
}
