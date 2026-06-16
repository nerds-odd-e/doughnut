package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.batches.Batch;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchOutputCollectionServiceTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchOutputCollectionService outputCollectionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch completedBatch;
  QuestionGenerationBatchRequest firstRequest;
  QuestionGenerationBatchRequest secondRequest;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");

    Note firstNote = makeMe.aNote().notebookOwnedBy(user).please();
    Note secondNote = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe
        .aMemoryTrackerFor(firstNote)
        .by(user)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();
    makeMe
        .aMemoryTrackerFor(secondNote)
        .by(user)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();

    QuestionGenerationBatch plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
    when(openAiApiHandler.uploadBatchInputFile(org.mockito.ArgumentMatchers.any()))
        .thenReturn("file-abc");
    when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-openai-1");
    submissionService.submitPlannedBatch(plannedBatch, currentTime);

    completedBatch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
    completedBatch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    batchRepository.saveAndFlush(completedBatch);

    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(completedBatch.getId());
    firstRequest = requests.get(0);
    secondRequest = requests.get(1);
  }

  private Batch completedOpenAiBatch() {
    return Batch.builder()
        .id("batch-openai-1")
        .completionWindow("24h")
        .createdAt(1L)
        .endpoint("/v1/responses")
        .inputFileId("file-abc")
        .outputFileId("file-output")
        .errorFileId("file-error")
        .status(Batch.Status.COMPLETED)
        .build();
  }

  private String successLine(String customId) {
    return """
        {"id":"batch_req_1","custom_id":"%s","response":{"status_code":200,"body":{"id":"resp-1"}},"error":null}"""
        .formatted(customId);
  }

  private String errorLine(String customId, String message) {
    return """
        {"id":"batch_req_2","custom_id":"%s","response":null,"error":{"code":"invalid_request_error","message":"%s"}}"""
        .formatted(customId, message);
  }

  @Nested
  class OutputMapping {
    @Test
    void mapsUnorderedOutputLinesToRequestRows() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
      when(openAiApiHandler.downloadFileContent("file-output"))
          .thenReturn(
              successLine(secondRequest.getCustomId())
                  + "\n"
                  + successLine(firstRequest.getCustomId()));
      when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      QuestionGenerationBatchRequest reloadedFirst =
          batchRequestRepository.findById(firstRequest.getId()).orElseThrow();
      QuestionGenerationBatchRequest reloadedSecond =
          batchRequestRepository.findById(secondRequest.getId()).orElseThrow();

      assertThat(reloadedFirst.getStatus(), is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
      assertThat(reloadedSecond.getStatus(), is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
      assertThat(reloadedFirst.getRawSuccessPayload(), is(successLine(firstRequest.getCustomId())));
      assertThat(
          reloadedSecond.getRawSuccessPayload(), is(successLine(secondRequest.getCustomId())));
      assertThat(reloadedFirst.getRawErrorPayload(), is(nullValue()));
      assertThat(reloadedSecond.getRawErrorPayload(), is(nullValue()));

      QuestionGenerationBatch reloadedBatch =
          batchRepository.findById(completedBatch.getId()).orElseThrow();
      assertThat(reloadedBatch.getOpenaiOutputFileId(), is("file-output"));
      assertThat(reloadedBatch.getOpenaiErrorFileId(), is("file-error"));
      assertThat(reloadedBatch.getOutputCollectedAt(), is(currentTime));
    }

    @Test
    void mapsErrorFileRowsToFailedRequests() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
      when(openAiApiHandler.downloadFileContent("file-output"))
          .thenReturn(successLine(firstRequest.getCustomId()));
      when(openAiApiHandler.downloadFileContent("file-error"))
          .thenReturn(errorLine(secondRequest.getCustomId(), "model unavailable"));

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      QuestionGenerationBatchRequest reloadedFirst =
          batchRequestRepository.findById(firstRequest.getId()).orElseThrow();
      QuestionGenerationBatchRequest reloadedSecond =
          batchRequestRepository.findById(secondRequest.getId()).orElseThrow();

      assertThat(reloadedFirst.getStatus(), is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
      assertThat(reloadedSecond.getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(
          reloadedSecond.getRawErrorPayload(),
          is(errorLine(secondRequest.getCustomId(), "model unavailable")));
      assertThat(reloadedSecond.getErrorDetail(), is("model unavailable"));
    }

    @Test
    void marksMissingOutputLinesAsFailed() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
      when(openAiApiHandler.downloadFileContent("file-output"))
          .thenReturn(successLine(firstRequest.getCustomId()));
      when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      QuestionGenerationBatchRequest reloadedSecond =
          batchRequestRepository.findById(secondRequest.getId()).orElseThrow();
      assertThat(reloadedSecond.getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(reloadedSecond.getErrorDetail(), is("missing batch output line"));
    }
  }

  @Nested
  class MalformedLines {
    @Test
    void ignoresMalformedOutputLinesWithoutFailingOtherRows() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
      when(openAiApiHandler.downloadFileContent("file-output"))
          .thenReturn(
              "not-json\n"
                  + successLine(firstRequest.getCustomId())
                  + "\n"
                  + "{\"response\":{\"status_code\":200}}");
      when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      List<QuestionGenerationBatchRequestStatus> statuses =
          batchRequestRepository.findByBatch_Id(completedBatch.getId()).stream()
              .map(QuestionGenerationBatchRequest::getStatus)
              .toList();
      assertThat(
          statuses,
          containsInAnyOrder(
              QuestionGenerationBatchRequestStatus.OUTPUT_READY,
              QuestionGenerationBatchRequestStatus.FAILED));
    }

    @Test
    void ignoresErrorLinesWithMissingCustomId() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
      when(openAiApiHandler.downloadFileContent("file-output")).thenReturn("");
      when(openAiApiHandler.downloadFileContent("file-error"))
          .thenReturn("{\"error\":{\"message\":\"batch failed\"}}");

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      List<QuestionGenerationBatchRequestStatus> statuses =
          batchRequestRepository.findByBatch_Id(completedBatch.getId()).stream()
              .map(QuestionGenerationBatchRequest::getStatus)
              .toList();
      assertThat(
          statuses,
          containsInAnyOrder(
              QuestionGenerationBatchRequestStatus.FAILED,
              QuestionGenerationBatchRequestStatus.FAILED));
    }
  }

  @Nested
  class CollectionScope {
    @Test
    void doesNotCollectAlreadyCollectedBatches() {
      completedBatch.setOutputCollectedAt(currentTime);
      batchRepository.saveAndFlush(completedBatch);

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      verify(openAiApiHandler, never()).retrieveBatch(anyString());
    }

    @Test
    void doesNotCollectNonCompletedBatches() {
      completedBatch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
      batchRepository.saveAndFlush(completedBatch);

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      verify(openAiApiHandler, never()).retrieveBatch(eq("batch-openai-1"));
    }
  }
}
