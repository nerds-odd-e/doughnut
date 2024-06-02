package com.odde.doughnut.testability;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import java.util.Objects;
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

  private void mockChatCompletion(String functionName, ChatCompletionResult toBeReturned) {
    if (Objects.equals(functionName, askClarificationQuestion)) {
      throw new RuntimeException();
    } else {
      Mockito.doReturn(Single.just(toBeReturned))
          .when(openAiApi)
          .createChatCompletion(ArgumentMatchers.argThat(request -> request.getTools() != null));
    }
  }
}
