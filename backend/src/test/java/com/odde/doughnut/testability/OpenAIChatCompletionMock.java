package com.odde.doughnut.testability;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import java.util.ArrayList;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIChatCompletionMock(OpenAiApi openAiApi) {
  public void mockChatCompletionAndReturnToolCall(Object result, String functionName) {
    mockChatCompletionAndReturnToolCallJsonNode(
        new ObjectMapperConfig().objectMapper().valueToTree(result), functionName);
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
            .choice(new ObjectMapperConfig().objectMapper().valueToTree(result).toString())
            .please();

    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(
            ArgumentMatchers.argThat(
                request -> request.getTools() == null || request.getTools().isEmpty()));
  }

  public void mockNullChatCompletion() {
    ChatCompletionResult emptyResult = new ChatCompletionResult();
    emptyResult.setChoices(new ArrayList<>());
    Mockito.doReturn(Single.just(emptyResult))
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
