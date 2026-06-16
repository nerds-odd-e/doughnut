package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.batches.Batch;
import java.sql.Timestamp;
import java.util.List;
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
class QuestionGenerationBatchMaintenanceServiceTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired QuestionGenerationBatchRowImportService rowImportService;
  @Autowired QuestionGenerationBatchRetentionService retentionService;
  @Autowired QuestionGenerationBatchMetrics batchMetrics;
  @Autowired RecallPromptRepository recallPromptRepository;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch submittedBatch;
  QuestionGenerationBatchRequest request;
  MCQWithAnswer mcqWithAnswer;
  QuestionGenerationBatchMaintenanceService maintenanceService;

  @BeforeEach
  void setup() {
    maintenanceService =
        new QuestionGenerationBatchMaintenanceService(
            new QuestionGenerationBatchPollingService(
                batchRepository, openAiApiHandler, batchMetrics),
            new QuestionGenerationBatchOutputCollectionService(
                batchRepository, batchRequestRepository, openAiApiHandler, batchMetrics),
            new QuestionGenerationBatchImportService(
                batchRepository, batchRequestRepository, rowImportService, batchMetrics),
            retentionService);

    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();

    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    MemoryTracker memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    submittedBatch = new QuestionGenerationBatch();
    submittedBatch.setUser(user);
    submittedBatch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
    submittedBatch.setPlannedAt(currentTime);
    submittedBatch.setSubmittedAt(currentTime);
    submittedBatch.setOpenaiBatchId("batch-openai-1");
    submittedBatch.setOpenaiInputFileId("file-abc");
    submittedBatch = batchRepository.saveAndFlush(submittedBatch);

    request = new QuestionGenerationBatchRequest();
    request.setBatch(submittedBatch);
    request.setMemoryTracker(memoryTracker);
    request.setContextSeed(42L);
    request.setCustomId(
        QuestionGenerationBatchRequest.customIdFor(submittedBatch.getId(), memoryTracker.getId()));
    request.setStatus(QuestionGenerationBatchRequestStatus.PENDING);
    batchRequestRepository.saveAndFlush(request);

    mcqWithAnswer =
        makeMe
            .aMCQWithAnswer()
            .stem("What color is the sky on a clear day?")
            .choices("Blue", "Green", "Red")
            .correctChoiceIndex(0)
            .please();

    when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
    when(openAiApiHandler.downloadFileContent("file-output"))
        .thenReturn(successLine(request.getCustomId()));
    when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");
    when(openAiApiHandler.parseStructuredOutputFromBatchSuccessLine(anyString(), any(Class.class)))
        .thenReturn(Optional.of(mcqWithAnswer));
  }

  @Test
  void resumesPollingOutputCollectionAndImportFromPersistedState() {
    maintenanceService.resumeExistingBatches(currentTime);

    QuestionGenerationBatch reloadedBatch =
        batchRepository.findById(submittedBatch.getId()).orElseThrow();
    assertThat(reloadedBatch.getStatus(), is(QuestionGenerationBatchStatus.COMPLETED));
    assertThat(reloadedBatch.getOutputCollectedAt(), is(currentTime));
    assertThat(reloadedBatch.getImportedAt(), is(currentTime));

    QuestionGenerationBatchRequest reloadedRequest =
        batchRequestRepository.findById(request.getId()).orElseThrow();
    assertThat(reloadedRequest.getStatus(), is(QuestionGenerationBatchRequestStatus.IMPORTED));

    List<RecallPrompt> recallPrompts =
        recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(
            request.getMemoryTracker().getId());
    assertThat(recallPrompts.size(), is(1));
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
}
