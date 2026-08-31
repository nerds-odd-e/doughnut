package com.odde.donut.services;

import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionGenerationBatchRetentionService {
  private static final Logger logger =
      LoggerFactory.getLogger(QuestionGenerationBatchRetentionService.class);

  static final Duration DEFAULT_RETENTION_WINDOW = Duration.ofDays(30);

  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;

  public QuestionGenerationBatchRetentionService(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
  }

  @Transactional
  public void pruneTerminalBatches(Timestamp currentTime) {
    pruneTerminalBatches(currentTime, DEFAULT_RETENTION_WINDOW);
  }

  void pruneTerminalBatches(Timestamp currentTime, Duration retentionWindow) {
    Timestamp cutoff = Timestamp.from(currentTime.toInstant().minus(retentionWindow));
    List<QuestionGenerationBatch> prunableBatches =
        batchRepository.findPrunableTerminalBatchesOlderThan(
            QuestionGenerationBatchStatus.openAiFailureRetryStatuses(),
            QuestionGenerationBatchStatus.COMPLETED,
            cutoff);
    if (prunableBatches.isEmpty()) {
      return;
    }

    logger.info(
        "Pruning {} terminal question generation batches older than {}",
        prunableBatches.size(),
        cutoff);
    for (QuestionGenerationBatch batch : prunableBatches) {
      batchRequestRepository.deleteByBatch_Id(batch.getId());
    }
    batchRepository.deleteAll(prunableBatches);
  }
}
