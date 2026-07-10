package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.batches.Batch;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchMetricsTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired MeterRegistry meterRegistry;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchPollingService pollingService;
  @Autowired QuestionGenerationBatchOutputCollectionService outputCollectionService;
  @Autowired QuestionGenerationBatchImportService batchImportService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;
  private double submittedBaseline;
  private double completedBaseline;
  private double importedBaseline;
  private double failedRowsBaseline;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");
    submittedBaseline = counter("question_generation_batch.submitted");
    completedBaseline = counter("question_generation_batch.completed");
    importedBaseline = counter("question_generation_batch.imported");
    failedRowsBaseline = counter("question_generation_batch_request.failed");
  }

  @Test
  void incrementsCountersForRepresentativeBatchLifecycleOutcomes() {
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe
        .aMemoryTrackerFor(note)
        .by(user)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();

    QuestionGenerationBatch plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
    when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
    when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-openai-1");
    submissionService.submitPlannedBatch(plannedBatch, currentTime);

    QuestionGenerationBatchRequest request =
        batchRequestRepository.findByBatch_Id(plannedBatch.getId()).getFirst();
    String customId = request.getCustomId();

    when(openAiApiHandler.retrieveBatch("batch-openai-1"))
        .thenReturn(
            Batch.builder()
                .id("batch-openai-1")
                .completionWindow("24h")
                .createdAt(1L)
                .endpoint("/v1/responses")
                .inputFileId("file-abc")
                .outputFileId("file-output")
                .errorFileId("file-error")
                .status(Batch.Status.COMPLETED)
                .build());
    pollingService.pollSubmittedBatches();

    when(openAiApiHandler.downloadFileContent("file-output"))
        .thenReturn(
            """
            {"id":"batch_req_1","custom_id":"%s","response":null,"error":{"message":"row failed"}}
            """
                .formatted(customId));
    when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");
    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    MCQWithAnswer mcqWithAnswer =
        makeMe
            .aMCQWithAnswer()
            .stem("What color is the sky on a clear day?")
            .choices("Blue", "Green", "Red")
            .correctChoiceIndex(0)
            .please();
    when(openAiApiHandler.parseStructuredOutputFromBatchSuccessLine(anyString(), any(Class.class)))
        .thenReturn(Optional.of(mcqWithAnswer));
    batchImportService.importCompletedBatches(currentTime);

    assertThat(counter("question_generation_batch.submitted") - submittedBaseline, is(1.0));
    assertThat(counter("question_generation_batch.completed") - completedBaseline, is(1.0));
    assertThat(counter("question_generation_batch.imported") - importedBaseline, is(1.0));
    assertThat(counter("question_generation_batch_request.failed") - failedRowsBaseline, is(1.0));
  }

  private double counter(String name) {
    return meterRegistry.get(name).counter().count();
  }
}
