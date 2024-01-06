package com.odde.doughnut.testability;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.runs.*;
import io.reactivex.Single;
import java.util.List;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIAssistantMock(OpenAiApi openAiApi) {
  public void mockChatCompletionAndReturnFunctionCall(Object result, String functionName) {
    mockChatCompletionAndReturnFunctionCallJsonNode(
        new ObjectMapper().valueToTree(result), functionName);
  }

  public void mockChatCompletionAndReturnFunctionCallJsonNode(
      JsonNode arguments, String functionName) {
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    mockChatCompletion(
        functionName,
        makeMe.openAiCompletionResult().functionCall(functionName, arguments).please());
  }

  private void mockChatCompletion(String functionName, ChatCompletionResult toBeReturned) {
    Mockito.doReturn(Single.just(new Run()))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
    Run retrievedRun = new Run();
    if (functionName == askClarificationQuestion) {
      retrievedRun.setStatus("requires_action");
      retrievedRun.setRequiredAction(
          RequiredAction.builder()
              .submitToolOutputs(
                  SubmitToolOutputs.builder()
                      .toolCalls(
                          List.of(
                              ToolCall.builder()
                                  .id("mocked-tool-call-id")
                                  .function(
                                      ToolCallFunction.builder()
                                          .name(askClarificationQuestion)
                                          .arguments(
                                              toBeReturned
                                                  .getChoices()
                                                  .get(0)
                                                  .getMessage()
                                                  .getFunctionCall()
                                                  .getArguments()
                                                  .toString())
                                          .build())
                                  .build()))
                      .build())
              .build());
    } else {
      Mockito.doReturn(Single.just(toBeReturned))
          .when(openAiApi)
          .createChatCompletion(ArgumentMatchers.any());
      retrievedRun.setStatus("completed");
    }
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}
