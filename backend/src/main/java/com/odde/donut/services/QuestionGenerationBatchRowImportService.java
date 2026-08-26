package com.odde.donut.services;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionType;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.GeneratedQuestionPostProcessor;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionGenerationBatchRowImportService {
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final EntityPersister entityPersister;
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchMetrics batchMetrics;
  private final GeneratedQuestionPostProcessor generatedQuestionPostProcessor;

  public QuestionGenerationBatchRowImportService(
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      EntityPersister entityPersister,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchMetrics batchMetrics,
      GeneratedQuestionPostProcessor generatedQuestionPostProcessor) {
    this.batchRequestRepository = batchRequestRepository;
    this.entityPersister = entityPersister;
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

    GeneratedMcq generatedMcq =
        openAiApiHandler
            .parseStructuredOutputFromBatchSuccessLine(
                request.getRawSuccessPayload(), GeneratedMcq.class)
            .filter(GeneratedMcq::isValid)
            .orElse(null);
    if (generatedMcq == null) {
      request.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
      request.setErrorDetail("invalid batch success payload");
      batchRequestRepository.save(request);
      batchMetrics.recordFailedRow();
      return false;
    }

    MemoryTracker memoryTracker = request.getMemoryTracker();
    Note note = memoryTracker.getNote();
    Mcq mcq =
        generatedQuestionPostProcessor.assembleMcq(generatedMcq, note, request.getContextSeed());
    entityPersister.save(mcq);

    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setMcq(mcq);
    recallPrompt.setMemoryTracker(memoryTracker);
    recallPrompt.setQuestionType(QuestionType.MCQ);
    entityPersister.save(recallPrompt);

    request.setStatus(QuestionGenerationBatchRequestStatus.IMPORTED);
    batchRequestRepository.save(request);
    return true;
  }
}
