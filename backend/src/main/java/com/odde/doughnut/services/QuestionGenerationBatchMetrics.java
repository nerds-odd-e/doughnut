package com.odde.doughnut.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchMetrics {
  private final Counter submittedBatches;
  private final Counter failedBatches;
  private final Counter expiredBatches;
  private final Counter completedBatches;
  private final Counter importedBatches;
  private final Counter failedRows;

  public QuestionGenerationBatchMetrics(MeterRegistry meterRegistry) {
    submittedBatches =
        Counter.builder("question_generation_batch.submitted")
            .description("Question generation batches accepted by OpenAI")
            .register(meterRegistry);
    failedBatches =
        Counter.builder("question_generation_batch.failed")
            .description(
                "Question generation batches that failed before or during OpenAI processing")
            .register(meterRegistry);
    expiredBatches =
        Counter.builder("question_generation_batch.expired")
            .description("Question generation batches that expired in OpenAI")
            .register(meterRegistry);
    completedBatches =
        Counter.builder("question_generation_batch.completed")
            .description("Question generation batches that completed in OpenAI")
            .register(meterRegistry);
    importedBatches =
        Counter.builder("question_generation_batch.imported")
            .description("Question generation batches fully imported into recall prompts")
            .register(meterRegistry);
    failedRows =
        Counter.builder("question_generation_batch_request.failed")
            .description(
                "Question generation batch rows that failed during output collection or import")
            .register(meterRegistry);
  }

  public void recordSubmittedBatch() {
    submittedBatches.increment();
  }

  public void recordFailedBatch() {
    failedBatches.increment();
  }

  public void recordExpiredBatch() {
    expiredBatches.increment();
  }

  public void recordCompletedBatch() {
    completedBatches.increment();
  }

  public void recordImportedBatch() {
    importedBatches.increment();
  }

  public void recordFailedRow() {
    failedRows.increment();
  }
}
