package com.odde.doughnut.testability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIChatCompletionMock(OpenAiApi openAiApi) {
  public void mockChatCompletionAndReturnFunctionCall(String functionName, Object result) {
    mockChatCompletionAndReturnFunctionCallJsonNode(
        functionName, new ObjectMapper().valueToTree(result));
  }

  public void mockChatCompletionAndReturnFunctionCallJsonNode(
      String functionName, JsonNode arguments) {
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    mockChatCompletionAndMatchFunctionCall(
        functionName, makeMe.openAiCompletionResult().functionCall("", arguments).please());
  }

  void mockChatCompletionAndMatchFunctionCall(
      String functionName, ChatCompletionResult toBeReturned) {
    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(
            ArgumentMatchers.argThat(
                request ->
                    request.getFunctions() != null
                        && request.getFunctions().get(0).getName().equals(functionName)));
  }
}
