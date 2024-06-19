package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIAssistantThreadMocker(OpenAiApi openAiApi, String threadId) {
  public OpenAIAssistantThreadMocker mockCreateMessage() {
    when(openAiApi.createMessage(eq(threadId), ArgumentMatchers.any()))
        .thenReturn(Single.just(new Message()));
    return this;
  }

  public void mockCreateRunInProcess(String runId) {
    Run run = new Run();
    run.setId(runId);
    run.setStatus("processing");
    Mockito.doReturn(Single.just(run))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}
