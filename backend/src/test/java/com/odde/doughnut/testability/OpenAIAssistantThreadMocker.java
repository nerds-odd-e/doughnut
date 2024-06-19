package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import org.mockito.ArgumentMatchers;

public record OpenAIAssistantThreadMocker(OpenAiApi openAiApi, String threadId) {
  public void mockCreateMessage() {
    when(openAiApi.createMessage(eq("this-thread"), ArgumentMatchers.any()))
        .thenReturn(Single.just(new Message()));
  }
}
