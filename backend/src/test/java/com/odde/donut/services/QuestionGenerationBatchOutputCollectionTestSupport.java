package com.odde.donut.services;

import com.odde.donut.testability.OpenAiBatchApiMock;
import com.openai.models.batches.Batch;

final class QuestionGenerationBatchOutputCollectionTestSupport {

  private QuestionGenerationBatchOutputCollectionTestSupport() {}

  /** Stubs OpenAI reporting batch-openai-1 completed with the given output and error files. */
  static void stubCompletedOpenAiBatch(
      OpenAiBatchApiMock openAiBatches, String outputLines, String errorLines) {
    openAiBatches.stubRetrieve(
        Batch.builder()
            .id("batch-openai-1")
            .completionWindow("24h")
            .createdAt(1L)
            .endpoint("/v1/responses")
            .inputFileId("file-abc")
            .outputFileId("file-output")
            .errorFileId("file-error")
            .status(Batch.Status.COMPLETED)
            .build());
    openAiBatches.stubFileContent("file-output", outputLines);
    openAiBatches.stubFileContent("file-error", errorLines);
  }

  static String successLine(String customId) {
    return """
        {"id":"batch_req_1","custom_id":"%s","response":{"status_code":200,"body":{"id":"resp-1"}},"error":null}"""
        .formatted(customId);
  }

  static String errorLine(String customId, String message) {
    return """
        {"id":"batch_req_2","custom_id":"%s","response":null,"error":{"code":"invalid_request_error","message":"%s"}}"""
        .formatted(customId, message);
  }
}
