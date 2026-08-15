package com.odde.doughnut.services;

import static com.odde.doughnut.services.QuestionGenerationBatchImportPayloadSupport.batchSuccessLine;
import static com.odde.doughnut.services.QuestionGenerationBatchRowImportAtomicTestSupport.FAIL_ON_RECALL_PROMPT_SAVE;
import static com.odde.doughnut.services.QuestionGenerationBatchRowImportAtomicTestSupport.deleteCommittedAtomicImportFixtures;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.testability.MakeMe;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles({"test", "batch-row-import-atomic-test"})
@Import(QuestionGenerationBatchRowImportAtomicTestSupport.FailingRecallPromptImportConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionGenerationBatchRowImportServiceAtomicTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRowImportService rowImportService;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @AfterEach
  void cleanupCommittedState() {
    FAIL_ON_RECALL_PROMPT_SAVE.set(false);
    inCommittedTransaction(() -> deleteCommittedAtomicImportFixtures(entityManager));
  }

  @Test
  void failureAfterQuestionCreationRollsBackAndLeavesRowReimportable() {
    CommittedImportFixture fixture = createCommittedImportFixture();

    FAIL_ON_RECALL_PROMPT_SAVE.set(true);
    QuestionGenerationBatchRequest request =
        batchRequestRepository.findById(fixture.requestId()).orElseThrow();

    assertThrows(RuntimeException.class, () -> rowImportService.importRow(request));

    inCommittedTransaction(
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
          assertThat(countMcqsForNote(fixture.noteId()), is(0L));
        });

    FAIL_ON_RECALL_PROMPT_SAVE.set(false);

    assertThat(rowImportService.importRow(request), is(true));

    inCommittedTransaction(
        () -> {
          assertThat(
              recallPromptRepository
                  .findAllByMemoryTracker_IdOrderByIdDesc(fixture.memoryTrackerId())
                  .size(),
              is(1));
          assertThat(countMcqsForNote(fixture.noteId()), is(1L));
        });
  }

  private CommittedImportFixture createCommittedImportFixture() {
    return inCommittedTransaction(
        () -> {
          try {
            String identifier = "batch-import-atomic-" + UUID.randomUUID();
            User committedUser = new User();
            committedUser.setExternalIdentifier(identifier);
            committedUser.setName(identifier);
            makeMe.entityPersister.save(committedUser);
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

            GeneratedMcq committedMcq =
                makeMe
                    .aGeneratedMcq()
                    .stem("What color is the sky on a clear day?")
                    .choices("Blue", "Green", "Red")
                    .correctAnswerIndex(0)
                    .please();

            QuestionGenerationBatchRequest request =
                makeMe
                    .aQuestionGenerationBatchRequest()
                    .batch(batch)
                    .memoryTracker(committedMemoryTracker)
                    .status(QuestionGenerationBatchRequestStatus.OUTPUT_READY)
                    .please();
            request.setRawSuccessPayload(batchSuccessLine(request.getCustomId(), committedMcq));
            batchRequestRepository.saveAndFlush(request);

            return new CommittedImportFixture(
                request.getId(), committedMemoryTracker.getId(), note.getId());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private long countMcqsForNote(int noteId) {
    return entityManager
        .createQuery("SELECT COUNT(mcq) FROM Mcq mcq WHERE mcq.note.id = :noteId", Long.class)
        .setParameter("noteId", noteId)
        .getSingleResult();
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

  private record CommittedImportFixture(int requestId, int memoryTrackerId, int noteId) {}
}
