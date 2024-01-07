package com.odde.doughnut.testability;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

  public void mockThreadCompletion(Object result, String functionName, String runId) {
    mockCreateRunInProcess(runId);
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    ChatCompletionResult toBeReturned =
        makeMe.openAiCompletionResult().functionCall(functionName, arguments).please();
    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(ArgumentMatchers.any());
    Run retrievedRun = getRunThatCompleted(runId);
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  public void mockSubmitOutputAndCompletion(Object result, String functionName, String runId) {
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();
    ChatCompletionResult toBeReturned =
        makeMe.openAiCompletionResult().functionCall(functionName, arguments).please();
    Mockito.doReturn(Single.just(toBeReturned))
        .when(openAiApi)
        .createChatCompletion(ArgumentMatchers.any());
    Run run = getRunThatCompleted(runId);
    when(openAiApi.submitToolOutputs(any(), any(), any())).thenReturn(Single.just(run));
  }

  public void mockSubmitOutputAndRequiredMoreAction(Object result, String runId) {
    Run run = getRunThatRequiresAction(new ObjectMapper().valueToTree(result).toString(), runId);
    when(openAiApi.submitToolOutputs(any(), any(), any())).thenReturn(Single.just(run));
  }

  private static Run getRunThatCompleted(String runId) {
    Run retrievedRun = new Run();
    retrievedRun.setId(runId);
    retrievedRun.setStatus("completed");
    return retrievedRun;
  }

  private void mockCreateRunInProcess(String runId) {
    Run run = new Run();
    run.setId(runId);
    run.setStatus("processing");
    Mockito.doReturn(Single.just(run))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private static Run getRunThatRequiresAction(String arguments, String runId) {
    Run retrievedRun = new Run();
    retrievedRun.setId(runId);
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
    return retrievedRun;
  }
}
