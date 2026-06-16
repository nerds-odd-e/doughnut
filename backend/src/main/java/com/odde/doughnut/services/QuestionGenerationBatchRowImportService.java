package com.odde.doughnut.services;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionGenerationBatchRowImportService {
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final EntityPersister entityPersister;
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchMetrics batchMetrics;

  public QuestionGenerationBatchRowImportService(
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      EntityPersister entityPersister,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchMetrics batchMetrics) {
    this.batchRequestRepository = batchRequestRepository;
    this.entityPersister = entityPersister;
    this.openAiApiHandler = openAiApiHandler;
    this.batchMetrics = batchMetrics;
  }

  @Transactional
  public boolean importRow(QuestionGenerationBatchRequest request) {
    if (request.getStatus() == QuestionGenerationBatchRequestStatus.IMPORTED) {
      return false;
    }
    if (request.getStatus() != QuestionGenerationBatchRequestStatus.OUTPUT_READY) {
      return false;
    }

    MCQWithAnswer mcqWithAnswer =
        openAiApiHandler
            .parseStructuredOutputFromBatchSuccessLine(
                request.getRawSuccessPayload(), MCQWithAnswer.class)
            .filter(MCQWithAnswer::isValid)
            .orElse(null);
    if (mcqWithAnswer == null) {
      request.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
      request.setErrorDetail("invalid batch success payload");
      batchRequestRepository.save(request);
      batchMetrics.recordFailedRow();
      return false;
    }

    MemoryTracker memoryTracker = request.getMemoryTracker();
    Note note = memoryTracker.getNote();
    PredefinedQuestion predefinedQuestion =
        PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note, request.getContextSeed());
    entityPersister.save(predefinedQuestion);

    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setPredefinedQuestion(predefinedQuestion);
    recallPrompt.setMemoryTracker(memoryTracker);
    recallPrompt.setQuestionType(QuestionType.MCQ);
    entityPersister.save(recallPrompt);

    request.setStatus(QuestionGenerationBatchRequestStatus.IMPORTED);
    batchRequestRepository.save(request);
    return true;
  }
}
