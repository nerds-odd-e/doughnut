package com.odde.donut.services;

import static com.odde.donut.services.QuestionGenerationBatchPollingTestSupport.openAiBatchWithStatus;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.odde.donut.testability.OpenAiBatchApiMock;
import com.odde.donut.testability.SpringTestBase;
import com.openai.models.batches.Batch;
import com.openai.models.batches.BatchError;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QuestionGenerationBatchPollingServiceTest extends SpringTestBase {

  OpenAiBatchApiMock openAiBatches;

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
    openAiBatches = new OpenAiBatchApiMock(officialClient);
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
    openAiBatches.stubUpload("file-abc");
    openAiBatches.stubBatchCreation("file-abc", "batch-openai-1");
    submissionService.submitPlannedBatch(plannedBatch, currentTime);
    submittedBatch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
    failedBaseline = counter("question_generation_batch.failed");
    expiredBaseline = counter("question_generation_batch.expired");
  }

  private double counter(String name) {
    return meterRegistry.get(name).counter().count();
  }

  @Nested
  class OpenAiStatusUpdates {
    @Test
    void inProgressLeavesBatchSubmitted() {
      openAiBatches.stubRetrieve(openAiBatchWithStatus(Batch.Status.IN_PROGRESS));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.SUBMITTED));
      verify(openAiBatches.batches()).retrieve("batch-openai-1");
    }

    @Test
    void completedUpdatesLocalBatchAndPersistsFileIds() {
      openAiBatches.stubRetrieve(
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
      openAiBatches.stubRetrieve(
          openAiBatchWithStatus(Batch.Status.FAILED).toBuilder()
              .errors(
                  Batch.Errors.builder()
                      .data(
                          List.of(
                              BatchError.builder().message("Cannot find file file-abc").build()))
                      .build())
              .build());

      RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> pollingService.pollSubmittedBatches());

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
      assertThat(counter("question_generation_batch.failed") - failedBaseline, is(1.0));
      QuestionGenerationBatchRequest request = onlyRequest(batch);
      assertThat(request.getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(
          request.getErrorDetail(), is(QuestionGenerationBatchRequest.ERROR_OPENAI_BATCH_FAILED));
      assertThat(thrown.getMessage(), containsString("Cannot find file file-abc"));
    }

    @Test
    void failedWithoutOpenAiErrorsUsesGenericMessage() {
      openAiBatches.stubRetrieve(openAiBatchWithStatus(Batch.Status.FAILED));

      RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> pollingService.pollSubmittedBatches());

      assertThat(
          thrown.getMessage(),
          containsString(QuestionGenerationBatchRequest.ERROR_OPENAI_BATCH_FAILED));
    }

    @Test
    void expiredUpdatesLocalBatch() {
      openAiBatches.stubRetrieve(openAiBatchWithStatus(Batch.Status.EXPIRED));

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
      when(openAiBatches.batches().retrieve("batch-openai-1"))
          .thenThrow(new RuntimeException("cannot access valid purpose=batch input file_id"));

      RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> pollingService.pollSubmittedBatches());

      assertThat(
          thrown.getMessage(), containsString("cannot access valid purpose=batch input file_id"));
    }
  }

  private QuestionGenerationBatchRequest onlyRequest(QuestionGenerationBatch batch) {
    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(batch.getId());
    assertThat(requests, hasSize(1));
    return requests.get(0);
  }
}
