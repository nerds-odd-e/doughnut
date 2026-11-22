package com.odde.donut.services;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.GeneratedQuestionPostProcessor;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.services.openAiApis.OpenAiApiHandler.BatchStructuredOutput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionGenerationBatchRowImportService {
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final RecallPromptPersister recallPromptPersister;
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchMetrics batchMetrics;
  private final GeneratedQuestionPostProcessor generatedQuestionPostProcessor;

  public QuestionGenerationBatchRowImportService(
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      RecallPromptPersister recallPromptPersister,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchMetrics batchMetrics,
      GeneratedQuestionPostProcessor generatedQuestionPostProcessor) {
    this.batchRequestRepository = batchRequestRepository;
    this.recallPromptPersister = recallPromptPersister;
    this.openAiApiHandler = openAiApiHandler;
    this.batchMetrics = batchMetrics;
    this.generatedQuestionPostProcessor = generatedQuestionPostProcessor;
  }

  @Transactional
  public boolean importRow(QuestionGenerationBatchRequest request) {
    if (request.getStatus() == QuestionGenerationBatchRequestStatus.IMPORTED) {
      return false;
    }
    if (request.getStatus() != QuestionGenerationBatchRequestStatus.OUTPUT_READY) {
      return false;
    }

    BatchStructuredOutput<GeneratedMcq> result =
        openAiApiHandler
            .parseStructuredOutputFromBatchSuccessLine(
                request.getRawSuccessPayload(), GeneratedMcq.class)
            .filter(response -> response.output().isValid())
            .orElse(null);
    if (result == null) {
      request.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
      request.setErrorDetail("invalid batch success payload");
      batchRequestRepository.save(request);
      batchMetrics.recordFailedRow();
      return false;
    }

    MemoryTracker memoryTracker = request.getMemoryTracker();
    Mcq mcq = generatedQuestionPostProcessor.assembleMcq(result.output(), request.getContextSeed());
    mcq.setFocusNoteInput(
        QuestionGenerationBatchJsonlRenderer.focusNoteInputFrom(result.metadata()));
    recallPromptPersister.persistNewRecallPromptForMcq(mcq, memoryTracker);

    request.setStatus(QuestionGenerationBatchRequestStatus.IMPORTED);
    batchRequestRepository.save(request);
    return true;
  }
}
