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

  public void mockThreadAndRequiredAction(Object result, String functionName) {
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    mockRequireAction(arguments.toString());
  }

  public void mockThreadCompletion(Object result, String functionName) {
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    mockCompletion(makeMe.openAiCompletionResult().functionCall(functionName, arguments).please());
  }

  private void mockCompletion(ChatCompletionResult toBeReturned) {
    Mockito.doReturn(Single.just(new Run()))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
    Run retrievedRun = new Run();
    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(ArgumentMatchers.any());
    retrievedRun.setStatus("completed");
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private void mockRequireAction(String arguments) {
    Mockito.doReturn(Single.just(new Run()))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
    Run retrievedRun = new Run();
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
                                        .arguments(arguments)
                                        .build())
                                .build()))
                    .build())
            .build());
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}
