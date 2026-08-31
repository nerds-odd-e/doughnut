package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.odde.donut.testability.MakeMe;
import com.openai.models.batches.Batch;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchPollingServiceTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchPollingService pollingService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired MeterRegistry meterRegistry;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch submittedBatch;
  private double failedBaseline;
  private double expiredBaseline;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");

    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe
        .aMemoryTrackerFor(note)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();

    QuestionGenerationBatch plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
    when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
    when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-openai-1");
    submissionService.submitPlannedBatch(plannedBatch, currentTime);
    submittedBatch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
    failedBaseline = counter("question_generation_batch.failed");
    expiredBaseline = counter("question_generation_batch.expired");
  }

  private double counter(String name) {
    return meterRegistry.get(name).counter().count();
  }

  private Batch openAiBatchWithStatus(Batch.Status status) {
    return Batch.builder()
        .id("batch-openai-1")
        .completionWindow("24h")
        .createdAt(1L)
        .endpoint("/v1/responses")
        .inputFileId("file-abc")
        .status(status)
        .build();
  }

  @Nested
  class OpenAiStatusUpdates {
    @Test
    void inProgressLeavesBatchSubmitted() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.IN_PROGRESS));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.SUBMITTED));
      verify(openAiApiHandler).retrieveBatch("batch-openai-1");
    }

    @Test
    void completedUpdatesLocalBatchAndPersistsFileIds() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(
              openAiBatchWithStatus(Batch.Status.COMPLETED).toBuilder()
                  .outputFileId("file-output")
                  .errorFileId("file-error")
                  .build());

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.COMPLETED));
      assertThat(batch.getOpenaiOutputFileId(), is("file-output"));
      assertThat(batch.getOpenaiErrorFileId(), is("file-error"));
    }

    @Test
    void failedUpdatesLocalBatch() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.FAILED));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
      assertThat(counter("question_generation_batch.failed") - failedBaseline, is(1.0));
      QuestionGenerationBatchRequest request = onlyRequest(batch);
      assertThat(request.getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(
          request.getErrorDetail(), is(QuestionGenerationBatchRequest.ERROR_OPENAI_BATCH_FAILED));
    }

    @Test
    void expiredUpdatesLocalBatch() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.EXPIRED));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.EXPIRED));
      assertThat(counter("question_generation_batch.expired") - expiredBaseline, is(1.0));
      QuestionGenerationBatchRequest request = onlyRequest(batch);
      assertThat(request.getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(
          request.getErrorDetail(), is(QuestionGenerationBatchRequest.ERROR_OPENAI_BATCH_EXPIRED));
    }
  }

  @Nested
  class OpenAiRetrieveFailure {
    @Test
    void surfacesTheOpenAiErrorInsteadOfSwallowingIt() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenThrow(new RuntimeException("cannot access valid purpose=batch input file_id"));

      RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> pollingService.pollSubmittedBatches());

      assertThat(
          thrown.getMessage(), containsString("cannot access valid purpose=batch input file_id"));
    }
  }

  @Nested
  class TerminalBatchesAreNotPolled {
    @ParameterizedTest
    @EnumSource(
        value = QuestionGenerationBatchStatus.class,
        names = {"COMPLETED", "FAILED", "EXPIRED"})
    void terminalBatchIsNotPolledAgain(QuestionGenerationBatchStatus terminalStatus) {
      submittedBatch.setStatus(terminalStatus);
      batchRepository.saveAndFlush(submittedBatch);

      pollingService.pollSubmittedBatches();

      verify(openAiApiHandler, never()).retrieveBatch(anyString());
    }
  }

  @Nested
  class PollingIsolation {
    @Test
    void onlyPollsSubmittedBatchesAmongMixedStatuses() {
      makeMe
          .aQuestionGenerationBatch()
          .forUser(user)
          .status(QuestionGenerationBatchStatus.COMPLETED)
          .plannedAt(currentTime)
          .openaiBatchId("batch-completed")
          .please();
      makeMe.entityPersister.flush();

      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.IN_PROGRESS));

      pollingService.pollSubmittedBatches();

      verify(openAiApiHandler).retrieveBatch("batch-openai-1");
      verify(openAiApiHandler, never()).retrieveBatch(eq("batch-completed"));
    }
  }

  private QuestionGenerationBatchRequest onlyRequest(QuestionGenerationBatch batch) {
    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(batch.getId());
    assertThat(requests, hasSize(1));
    return requests.get(0);
  }
}
