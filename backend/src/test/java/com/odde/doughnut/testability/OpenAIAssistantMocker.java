package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;

import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import org.mockito.Mockito;

public record OpenAIAssistantMocker(OpenAiApi openAiApi) {
  public OpenAIAssistantThreadMocker mockThreadCreation(String threadId) {
    Thread item = new Thread();
    item.setId(threadId);
    Mockito.doReturn(Single.just(item)).when(openAiApi).createThread(any());
    return new OpenAIAssistantThreadMocker(openAiApi, threadId);
  }

  public OpenAIAssistantCreatedRunMocker aCreatedRun(String threadId, String runId) {
    return new OpenAIAssistantCreatedRunMocker(openAiApi, threadId, runId);
  }

  public OpenAIAssistantThreadMocker aThread(String threadId) {
    return new OpenAIAssistantThreadMocker(openAiApi, threadId);
  }
}
