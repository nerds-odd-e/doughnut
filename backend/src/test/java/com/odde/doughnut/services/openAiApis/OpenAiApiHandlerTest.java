package com.odde.doughnut.services.openAiApis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.runs.Run;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OpenAiApiHandlerTest {
  @Mock private OpenAiApi openAiApi;
  OpenAiApiHandler openAIChatCompletionMock;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    openAIChatCompletionMock = new OpenAiApiHandler(openAiApi);
    Run run = new Run();
    run.setId("runId");
    when(openAiApi.createRun(any(), any())).thenReturn(Single.just(run));
  }

  @Nested
  class BlockRetrieveRun {

    @Test
    void returnWhenCompleted() {
      Run retrievedRun = new Run();
      retrievedRun.setStatus("completed");
      when(openAiApi.retrieveRun("threadId", "runId")).thenReturn(Single.just(retrievedRun));
      assertEquals(
          "completed", openAIChatCompletionMock.blockGetRun("threadId", "runId").getStatus());
    }

    @Test
    void returnWhenActionRequired() {
      Run retrievedRun = new Run();
      retrievedRun.setStatus("requires_action");
      when(openAiApi.retrieveRun("threadId", "runId")).thenReturn(Single.just(retrievedRun));
      assertEquals(
          "requires_action", openAIChatCompletionMock.blockGetRun("threadId", "runId").getStatus());
    }

    @Test
    void stopAfterTryingMoreThan10Times() {
      Run retrievedRun = new Run();
      retrievedRun.setStatus("in_progress");
      when(openAiApi.retrieveRun("threadId", "runId")).thenReturn(Single.just(retrievedRun));
      assertEquals(
          "in_progress", openAIChatCompletionMock.blockGetRun("threadId", "runId").getStatus());
    }
  }
}
