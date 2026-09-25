package com.odde.donut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openai.client.OpenAIClient;
import com.openai.core.http.HttpResponse;
import com.openai.models.batches.Batch;
import com.openai.models.batches.BatchCreateParams;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.services.blocking.BatchService;
import com.openai.services.blocking.FileService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Stubs the OpenAI Files and Batches APIs of the mocked official client. */
public class OpenAiBatchApiMock {
  private final FileService files = mock(FileService.class);
  private final BatchService batches = mock(BatchService.class);

  public OpenAiBatchApiMock(OpenAIClient officialClient) {
    when(officialClient.files()).thenReturn(files);
    when(officialClient.batches()).thenReturn(batches);
  }

  public FileService files() {
    return files;
  }

  public BatchService batches() {
    return batches;
  }

  public void stubUpload(String fileId) {
    when(files.create(any(FileCreateParams.class)))
        .thenReturn(
            FileObject.builder()
                .id(fileId)
                .bytes(0)
                .createdAt(1L)
                .filename("batch-input.jsonl")
                .purpose(FileObject.Purpose.BATCH)
                .status(FileObject.Status.PROCESSED)
                .build());
  }

  public void stubBatchCreation(String inputFileId, String batchId) {
    when(batches.create(
            argThat((BatchCreateParams params) -> inputFileId.equals(params.inputFileId()))))
        .thenReturn(
            Batch.builder()
                .id(batchId)
                .completionWindow("24h")
                .createdAt(1L)
                .endpoint("/v1/responses")
                .inputFileId(inputFileId)
                .status(Batch.Status.VALIDATING)
                .build());
  }

  public void stubRetrieve(Batch batch) {
    when(batches.retrieve(batch.id())).thenReturn(batch);
  }

  public void stubFileContent(String fileId, String content) {
    HttpResponse response = mock(HttpResponse.class);
    when(response.body())
        .thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    when(files.content(fileId)).thenReturn(response);
  }
}
