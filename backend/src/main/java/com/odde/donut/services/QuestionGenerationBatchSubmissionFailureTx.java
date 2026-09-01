package com.odde.donut.services;

import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionGenerationBatchSubmissionFailureTx {
  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;

  public QuestionGenerationBatchSubmissionFailureTx(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void persistFailedSubmission(QuestionGenerationBatch batch, String errorMessage) {
    batch.setStatus(QuestionGenerationBatchStatus.FAILED);
    batchRequestRepository.markPendingAsFailedForBatch(
        batch.getId(),
        QuestionGenerationBatchRequest.ERROR_BATCH_SUBMISSION_FAILED + ": " + errorMessage);
    batchRepository.saveAndFlush(batch);
  }
}
