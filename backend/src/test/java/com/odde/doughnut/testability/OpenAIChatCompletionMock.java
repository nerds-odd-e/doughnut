package com.odde.doughnut.testability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.runs.Run;
import io.reactivex.Single;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIChatCompletionMock(OpenAiApi openAiApi) {
  public void mockChatCompletionAndReturnFunctionCall(Object result, String functionName) {
    mockChatCompletionAndReturnFunctionCallJsonNode(
        new ObjectMapper().valueToTree(result), functionName);
  }

  public void mockChatCompletionAndReturnFunctionCallJsonNode(
      JsonNode arguments, String functionName) {
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    mockChatCompletion(
        makeMe.openAiCompletionResult().functionCall(functionName, arguments).please());
  }

  void mockChatCompletion(ChatCompletionResult toBeReturned) {
    Mockito.doReturn(Single.just(new Run()))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
    Run retrievedRun = new Run();
    retrievedRun.setStatus("completed");
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(ArgumentMatchers.argThat(request -> request.getFunctions() != null));
  }
}
