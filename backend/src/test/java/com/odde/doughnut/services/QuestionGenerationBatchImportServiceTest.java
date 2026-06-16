package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchImportServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchImportService batchImportService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch batch;
  QuestionGenerationBatchRequest importableRequest;
  QuestionGenerationBatchRequest failedRequest;
  QuestionGenerationBatchRequest malformedRequest;
  QuestionGenerationBatchRequest alreadyImportedRequest;
  MCQWithAnswer mcqWithAnswer;

  @BeforeEach
  void setup() throws JsonProcessingException {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();

    batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    batch.setPlannedAt(currentTime);
    batch.setOutputCollectedAt(currentTime);
    batch = batchRepository.saveAndFlush(batch);

    importableRequest = createRequest("importable");
    failedRequest = createRequest("failed");
    malformedRequest = createRequest("malformed");
    alreadyImportedRequest = createRequest("already-imported");

    mcqWithAnswer =
        makeMe
            .aMCQWithAnswer()
            .stem("What color is the sky on a clear day?")
            .choices("Blue", "Green", "Red")
            .correctChoiceIndex(0)
            .please();

    importableRequest.setStatus(QuestionGenerationBatchRequestStatus.OUTPUT_READY);
    importableRequest.setRawSuccessPayload(
        batchSuccessLine(importableRequest.getCustomId(), mcqWithAnswer));

    failedRequest.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
    failedRequest.setErrorDetail("model unavailable");

    malformedRequest.setStatus(QuestionGenerationBatchRequestStatus.OUTPUT_READY);
    malformedRequest.setRawSuccessPayload(
        """
        {"id":"batch_req_1","custom_id":"%s","response":{"status_code":200,"body":{"id":"resp-1"}},"error":null}"""
            .formatted(malformedRequest.getCustomId()));

    alreadyImportedRequest.setStatus(QuestionGenerationBatchRequestStatus.IMPORTED);
    batchRequestRepository.saveAll(
        List.of(importableRequest, failedRequest, malformedRequest, alreadyImportedRequest));
  }

  private QuestionGenerationBatchRequest reloadRequest(QuestionGenerationBatchRequest request) {
    return batchRequestRepository.findById(request.getId()).orElseThrow();
  }

  private QuestionGenerationBatchRequest createRequest(String label) {
    Note note = makeMe.aNote().notebookOwnedBy(user).title(label).please();
    MemoryTracker memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    QuestionGenerationBatchRequest request = new QuestionGenerationBatchRequest();
    request.setBatch(batch);
    request.setMemoryTracker(memoryTracker);
    request.setContextSeed(42L);
    request.setCustomId(
        QuestionGenerationBatchRequest.customIdFor(batch.getId(), memoryTracker.getId()));
    return request;
  }

  @Nested
  class ImportCompletedBatch {
    @Test
    void importsSuccessfulRowsAndMarksBatchImportedWhenEveryRowIsTerminal() {
      batchImportService.importCompletedBatches(currentTime);

      assertThat(
          reloadRequest(importableRequest).getStatus(),
          is(QuestionGenerationBatchRequestStatus.IMPORTED));
      assertThat(
          reloadRequest(failedRequest).getStatus(),
          is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(reloadRequest(failedRequest).getErrorDetail(), is("model unavailable"));
      assertThat(
          reloadRequest(malformedRequest).getStatus(),
          is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(
          reloadRequest(malformedRequest).getErrorDetail(), is("invalid batch success payload"));
      assertThat(
          reloadRequest(alreadyImportedRequest).getStatus(),
          is(QuestionGenerationBatchRequestStatus.IMPORTED));

      QuestionGenerationBatch reloadedBatch = batchRepository.findById(batch.getId()).orElseThrow();
      assertThat(reloadedBatch.getImportedAt(), is(currentTime));

      List<RecallPrompt> recallPrompts =
          recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(
              importableRequest.getMemoryTracker().getId());
      assertThat(recallPrompts.size(), is(1));
    }

    @Test
    void reimportingAlreadyImportedBatchIsSafe() {
      batchImportService.importCompletedBatches(currentTime);
      batchImportService.importCompletedBatches(currentTime);

      QuestionGenerationBatch reloadedBatch = batchRepository.findById(batch.getId()).orElseThrow();
      assertThat(reloadedBatch.getImportedAt(), is(currentTime));

      List<RecallPrompt> recallPrompts =
          recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(
              importableRequest.getMemoryTracker().getId());
      assertThat(recallPrompts.size(), is(1));
    }
  }

  @Nested
  class ImportScope {
    @Test
    void doesNotImportBatchesWithoutCollectedOutput() {
      batch.setOutputCollectedAt(null);
      batchRepository.saveAndFlush(batch);

      batchImportService.importCompletedBatches(currentTime);

      QuestionGenerationBatch reloadedBatch = batchRepository.findById(batch.getId()).orElseThrow();
      assertThat(reloadedBatch.getImportedAt(), is(nullValue()));
      assertThat(
          reloadRequest(importableRequest).getStatus(),
          is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
    }

    @Test
    void doesNotImportAlreadyImportedBatches() {
      batch.setImportedAt(currentTime);
      batchRepository.saveAndFlush(batch);

      batchImportService.importCompletedBatches(currentTime);

      assertThat(
          reloadRequest(importableRequest).getStatus(),
          is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
    }
  }

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
