package com.odde.doughnut.testability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIChatCompletionMock(OpenAiApi openAiApi) {
  public void mockChatCompletionAndReturnFunctionCall(Object result) {
    mockChatCompletionAndReturnFunctionCallJsonNode(new ObjectMapper().valueToTree(result));
  }

  public void mockChatCompletionAndReturnFunctionCallJsonNode(JsonNode arguments) {
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    mockChatCompletion(makeMe.openAiCompletionResult().functionCall("", arguments).please());
  }

  void mockChatCompletion(ChatCompletionResult toBeReturned) {
    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(ArgumentMatchers.argThat(request -> request.getFunctions() != null));
  }
}
