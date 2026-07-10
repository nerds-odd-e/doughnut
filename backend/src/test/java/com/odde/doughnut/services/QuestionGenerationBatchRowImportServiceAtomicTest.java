package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles({"test", "batch-row-import-atomic-test"})
@Import(QuestionGenerationBatchRowImportServiceAtomicTest.FailingRecallPromptImportConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionGenerationBatchRowImportServiceAtomicTest {

  static final AtomicBoolean FAIL_ON_RECALL_PROMPT_SAVE = new AtomicBoolean(false);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRowImportService rowImportService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @AfterEach
  void cleanupCommittedState() {
    FAIL_ON_RECALL_PROMPT_SAVE.set(false);
    inCommittedTransaction(this::deleteCommittedAtomicImportFixtures);
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
          assertThat(countPredefinedQuestionsForNote(fixture.noteId()), is(0L));
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
          assertThat(countPredefinedQuestionsForNote(fixture.noteId()), is(1L));
        });
  }

  @TestConfiguration
  @Profile("batch-row-import-atomic-test")
  static class FailingRecallPromptImportConfig {
    @Bean
    @Primary
    EntityPersister entityPersister(EntityManager entityManager) {
      return new FailableEntityPersister(entityManager);
    }
  }

  static class FailableEntityPersister extends EntityPersister {
    FailableEntityPersister(EntityManager entityManager) {
      super(entityManager);
    }

    @Override
    public <T> T save(T entity) {
      if (FAIL_ON_RECALL_PROMPT_SAVE.get() && entity instanceof RecallPrompt) {
        throw new RuntimeException("forced failure after question creation");
      }
      return super.save(entity);
    }
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
                    .by(committedUser)
                    .nextRecallAt(
                        new Timestamp(committedTime.getTime() + TimeUnit.HOURS.toMillis(24)))
                    .please();

            QuestionGenerationBatch batch = new QuestionGenerationBatch();
            batch.setUser(committedUser);
            batch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
            batch.setPlannedAt(committedTime);
            batch = batchRepository.saveAndFlush(batch);

            MCQWithAnswer committedMcq =
                makeMe
                    .aMCQWithAnswer()
                    .stem("What color is the sky on a clear day?")
                    .choices("Blue", "Green", "Red")
                    .correctChoiceIndex(0)
                    .please();

            String customId =
                QuestionGenerationBatchRequest.customIdFor(
                    batch.getId(), committedMemoryTracker.getId());
            QuestionGenerationBatchRequest request = new QuestionGenerationBatchRequest();
            request.setBatch(batch);
            request.setMemoryTracker(committedMemoryTracker);
            request.setContextSeed(42L);
            request.setCustomId(customId);
            request.setStatus(QuestionGenerationBatchRequestStatus.OUTPUT_READY);
            request.setRawSuccessPayload(batchSuccessLine(customId, committedMcq));
            request = batchRequestRepository.saveAndFlush(request);

            return new CommittedImportFixture(
                request.getId(), committedMemoryTracker.getId(), note.getId());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private void deleteCommittedAtomicImportFixtures() {
    entityManager
        .createNativeQuery(
            "DELETE rp FROM recall_prompt rp "
                + "INNER JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE pq FROM predefined_question pq "
                + "INNER JOIN note n ON pq.note_id = n.id "
                + "INNER JOIN notebook nb ON n.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE qgr FROM question_generation_batch_request qgr "
                + "INNER JOIN question_generation_batch qgb ON qgr.batch_id = qgb.id "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE qgb FROM question_generation_batch qgb "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE mt FROM memory_tracker mt "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE n FROM note n "
                + "INNER JOIN notebook nb ON n.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE nb FROM notebook nb "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE o FROM ownership o "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE FROM user WHERE external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
  }

  private long countPredefinedQuestionsForNote(int noteId) {
    return entityManager
        .createQuery(
            "SELECT COUNT(pq) FROM PredefinedQuestion pq WHERE pq.note.id = :noteId", Long.class)
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

  private static String batchSuccessLine(String customId, MCQWithAnswer mcqWithAnswer)
      throws JsonProcessingException {
    String structuredOutput = OBJECT_MAPPER.writeValueAsString(mcqWithAnswer);
    String responseBody =
        """
        {
          "id": "resp-1",
          "status": "completed",
          "output": [
            {
              "type": "message",
              "id": "msg-1",
              "status": "completed",
              "content": [
                {
                  "type": "output_text",
                  "text": %s
                }
              ]
            }
          ]
        }
        """
            .formatted(OBJECT_MAPPER.writeValueAsString(structuredOutput));

    return """
        {"id":"batch_req_1","custom_id":"%s","response":{"status_code":200,"body":%s},"error":null}"""
        .formatted(customId, responseBody);
  }
}
