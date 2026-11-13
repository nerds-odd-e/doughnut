package com.odde.donut.services;

import static com.odde.donut.services.QuestionGenerationBatchImportPayloadSupport.batchSuccessLine;
import static com.odde.donut.services.QuestionGenerationBatchRowImportAtomicTestSupport.FAIL_ON_RECALL_PROMPT_SAVE;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Accepted cost: this profile needs its own Spring context boot (~1s, 2026-09-26); merging the
// injected failure into the shared context would spread mutable failure state, and a mock would
// lose the transaction rollback proof.
@SpringBootTest
@ActiveProfiles({"test", "batch-row-import-atomic-test"})
@Import(QuestionGenerationBatchRowImportAtomicTestSupport.FailingRecallPromptImportConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionGenerationBatchRowImportServiceAtomicTest {

  @Autowired MakeMe makeMe;
  @Autowired McqRepository mcqRepository;
  @Autowired QuestionGenerationBatchRowImportService rowImportService;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @AfterEach
  void cleanupCommittedState() {
    FAIL_ON_RECALL_PROMPT_SAVE.set(false);
    inCommittedTransaction(
        transactionManager,
        () ->
            deleteByUserExternalIdentifierLike(
                entityManager, mcqRepository, "batch-import-atomic-%"));
  }

  @Test
  void failureAfterQuestionCreationRollsBackAndLeavesRowReimportable() {
    CommittedImportFixture fixture = createCommittedImportFixture();

    FAIL_ON_RECALL_PROMPT_SAVE.set(true);
    QuestionGenerationBatchRequest request =
        batchRequestRepository.findById(fixture.requestId()).orElseThrow();

    assertThrows(RuntimeException.class, () -> rowImportService.importRow(request));

    inCommittedTransaction(
        transactionManager,
        () -> {
          QuestionGenerationBatchRequest reloadedRequest =
              batchRequestRepository.findById(fixture.requestId()).orElseThrow();
          assertThat(
              reloadedRequest.getStatus(), is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
          assertThat(
              recallPromptRepository
                  .findAllByMemoryTracker_IdOrderByIdDesc(fixture.memoryTrackerId())
                  .size(),
              is(0));
          assertThat(countMcqsForContextSeed(fixture.contextSeed()), is(0L));
        });

    FAIL_ON_RECALL_PROMPT_SAVE.set(false);

    assertThat(rowImportService.importRow(request), is(true));

    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(
              recallPromptRepository
                  .findAllByMemoryTracker_IdOrderByIdDesc(fixture.memoryTrackerId())
                  .size(),
              is(1));
          assertThat(countMcqsForContextSeed(fixture.contextSeed()), is(1L));
        });
  }

  private CommittedImportFixture createCommittedImportFixture() {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            User committedUser = makeMe.aUser("batch-import-atomic-" + UUID.randomUUID()).please();
            Timestamp committedTime = makeMe.aTimestamp().please();
            Note note = makeMe.aNote().notebookOwnedBy(committedUser).please();
            MemoryTracker committedMemoryTracker =
                makeMe
                    .aMemoryTrackerFor(note)
                    .nextRecallAt(
                        new Timestamp(committedTime.getTime() + TimeUnit.HOURS.toMillis(24)))
                    .please();

            QuestionGenerationBatch batch =
                makeMe
                    .aQuestionGenerationBatch()
                    .forUser(committedUser)
                    .status(QuestionGenerationBatchStatus.COMPLETED)
                    .plannedAt(committedTime)
                    .please();
            makeMe.entityPersister.flush();

            GeneratedMcq committedMcq = makeMe.aGeneratedMcq().please();

            long contextSeed = UUID.randomUUID().getMostSignificantBits();
            QuestionGenerationBatchRequest request =
                makeMe
                    .aQuestionGenerationBatchRequest()
                    .batch(batch)
                    .memoryTracker(committedMemoryTracker)
                    .contextSeed(contextSeed)
                    .status(QuestionGenerationBatchRequestStatus.OUTPUT_READY)
                    .please();
            request.setRawSuccessPayload(batchSuccessLine(request.getCustomId(), committedMcq));
            batchRequestRepository.saveAndFlush(request);

            return new CommittedImportFixture(
                request.getId(), committedMemoryTracker.getId(), contextSeed);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private long countMcqsForContextSeed(long contextSeed) {
    return entityManager
        .createQuery(
            "SELECT COUNT(mcq) FROM Mcq mcq WHERE mcq.contextSeed = :contextSeed", Long.class)
        .setParameter("contextSeed", contextSeed)
        .getSingleResult();
  }

  private record CommittedImportFixture(int requestId, int memoryTrackerId, long contextSeed) {}
}
