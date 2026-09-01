package com.odde.donut.services;

import com.openai.models.batches.Batch;

final class QuestionGenerationBatchPollingTestSupport {

  private QuestionGenerationBatchPollingTestSupport() {}

  static Batch openAiBatchWithStatus(Batch.Status status) {
    return Batch.builder()
        .id("batch-openai-1")
        .completionWindow("24h")
        .createdAt(1L)
        .endpoint("/v1/responses")
        .inputFileId("file-abc")
        .status(status)
        .build();
  }
}
