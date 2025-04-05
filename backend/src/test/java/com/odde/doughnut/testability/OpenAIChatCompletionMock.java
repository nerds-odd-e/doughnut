package com.odde.doughnut.testability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIChatCompletionMock(OpenAiApi openAiApi) {
  public void mockChatCompletionAndReturnToolCall(Object result, String functionName) {
    mockChatCompletionAndReturnToolCallJsonNode(
        new ObjectMapper().valueToTree(result), functionName);
  }

  public void mockChatCompletionAndReturnToolCallJsonNode(JsonNode arguments, String functionName) {
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    mockChatCompletion(
        functionName, makeMe.openAiCompletionResult().toolCall(functionName, arguments).please());
  }

  public void mockChatCompletionAndReturnJsonSchema(Object result) {
    if (result == null) {
      mockNullChatCompletion();
      return;
    }

    ChatCompletionResult toBeReturned =
        MakeMe.makeMeWithoutFactoryService()
            .openAiCompletionResult()
            .choice(new ObjectMapper().valueToTree(result).toString())
            .please();

    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(
            ArgumentMatchers.argThat(
                request -> request.getTools() == null || request.getTools().isEmpty()));
  }

  public void mockNullChatCompletion() {
    Mockito.doReturn(Single.just(new ChatCompletionResult()))
        .when(openAiApi)
        .createChatCompletion(
            ArgumentMatchers.argThat(
                request -> request.getTools() == null || request.getTools().isEmpty()));
  }

  private void mockChatCompletion(String functionName, ChatCompletionResult toBeReturned) {
    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(ArgumentMatchers.argThat(request -> request.getTools() != null));
  }
}
