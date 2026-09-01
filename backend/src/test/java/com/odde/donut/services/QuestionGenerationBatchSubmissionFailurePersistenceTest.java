package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.testability.CommittedUserCleanup;
import com.odde.donut.testability.MakeMe;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchSubmissionFailurePersistenceTest {

  static final String COMMITTED_USER_PREFIX = "batch-submit-fail-";

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired MeterRegistry meterRegistry;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired EntityManager entityManager;
  @Autowired PlatformTransactionManager transactionManager;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch plannedBatch;
  private double submittedBaseline;
  private double failedBaseline;

  @BeforeEach
  void setup() {
    submittedBaseline = counter("question_generation_batch.submitted");
    failedBaseline = counter("question_generation_batch.failed");
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");
    replaceWithCommittedPlannedBatch();
  }

  @AfterEach
  void cleanupCommittedState() {
    inCommittedTransaction(
        () ->
            CommittedUserCleanup.deleteByUserExternalIdentifierLike(
                entityManager, COMMITTED_USER_PREFIX + "%"));
  }

  @Nested
  class FirstTimeFailedSubmission {
    @BeforeEach
    void failBatchCreation() {
      when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
      when(openAiApiHandler.createResponsesBatch("file-abc"))
          .thenThrow(new RuntimeException("batch create failed"));
    }

    @Test
    void batchCreationFailureLeavesNoLatestSubmittedAt() {
      assertThrows(
          RuntimeException.class,
          () -> submissionService.submitPlannedBatch(plannedBatch, currentTime));

      inCommittedTransaction(
          () ->
              assertThat(
                  batchRepository.findLatestSubmittedAtByUser_Id(user.getId()).isPresent(),
                  is(false)));
    }

    @Test
    void localFailedRemainsAfterOpenAiExceptionIsThrown() {
      TransactionTemplate callingTx = new TransactionTemplate(transactionManager);
      callingTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      assertThrows(
          RuntimeException.class,
          () ->
              callingTx.executeWithoutResult(
                  status -> submissionService.submitPlannedBatch(plannedBatch, currentTime)));

      inCommittedTransaction(
          () ->
              assertThat(
                  batchRepository.findById(plannedBatch.getId()).orElseThrow().getStatus(),
                  is(QuestionGenerationBatchStatus.FAILED)));
    }
  }

  @Nested
  class FailedSubmission {
    Timestamp previousSubmission;

    @BeforeEach
    void setupPreviousSubmission() {
      previousSubmission = new Timestamp(currentTime.getTime() - TimeUnit.DAYS.toMillis(2));
      inCommittedTransaction(
          () ->
              makeMe
                  .aQuestionGenerationBatch()
                  .forUser(user)
                  .completedAt(previousSubmission)
                  .please());
    }

    @Test
    void uploadFailureMarksBatchFailedWithoutUpdatingLatestSubmittedAt() {
      when(openAiApiHandler.uploadBatchInputFile(any()))
          .thenThrow(new RuntimeException("upload failed"));

      assertThrows(
          RuntimeException.class,
          () -> submissionService.submitPlannedBatch(plannedBatch, currentTime));

      assertThat(counter("question_generation_batch.failed") - failedBaseline, is(1.0));
      assertThat(counter("question_generation_batch.submitted") - submittedBaseline, is(0.0));

      inCommittedTransaction(
          () -> {
            QuestionGenerationBatch batch =
                batchRepository.findById(plannedBatch.getId()).orElseThrow();
            assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
            assertThat(batch.getOpenaiInputFileId(), is(nullValue()));
            assertThat(batch.getOpenaiBatchId(), is(nullValue()));
            assertThat(batch.getSubmittedAt(), is(nullValue()));
            List<QuestionGenerationBatchRequest> requests =
                batchRequestRepository.findByBatch_Id(batch.getId());
            assertThat(requests, hasSize(1));
            assertThat(
                requests.get(0).getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
            assertThat(
                requests.get(0).getErrorDetail(),
                containsString(QuestionGenerationBatchRequest.ERROR_BATCH_SUBMISSION_FAILED));
            assertThat(requests.get(0).getErrorDetail(), containsString("upload failed"));
            assertThat(
                batchRepository.findLatestSubmittedAtByUser_Id(user.getId()).orElseThrow(),
                equalTo(previousSubmission));
          });
    }
  }

  private void replaceWithCommittedPlannedBatch() {
    inCommittedTransaction(
        () -> {
          user = uniqueCommittedUser();
          Note note = makeMe.aNote().notebookOwnedBy(user).please();
          makeMe
              .aMemoryTrackerFor(note)
              .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
              .please();
          plannedBatch = planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
        });
  }

  private User uniqueCommittedUser() {
    return makeMe.aUser(COMMITTED_USER_PREFIX + UUID.randomUUID()).please();
  }

  private void inCommittedTransaction(Runnable action) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    template.executeWithoutResult(status -> action.run());
  }

  private double counter(String name) {
    return meterRegistry.get(name).counter().count();
  }
}
