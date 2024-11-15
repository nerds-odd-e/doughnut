package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import org.mockito.Mockito;

public record OpenAIAssistantRunMocker(OpenAiApi openAiApi, String threadId, Run run) {
  public OpenAIAssistantRunMocker mockRetrieveRun() {
    Mockito.doReturn(Single.just(run)).when(openAiApi).retrieveRun(eq(threadId), any());
    return this;
  }

  public void mockSubmitOutput() {
    Mockito.doReturn(Single.just(run))
        .when(openAiApi)
        .submitToolOutputs(eq(threadId), any(), any());
  }

  public void mockCancelRun(String runId) {
    Mockito.doReturn(Single.just(run)).when(openAiApi).cancelRun(eq(threadId), eq(runId));
  }
}
