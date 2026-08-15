package com.odde.doughnut.services;

import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.ai.GeneratedQuestionPostProcessor;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
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
    GeneratedMcq postProcessedQuestion = generatedQuestionPostProcessor.postProcess(generatedMcq);
    Mcq mcq = postProcessedQuestion.toMcq(note, request.getContextSeed());
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
