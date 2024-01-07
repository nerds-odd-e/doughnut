package com.odde.doughnut.testability;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.runs.*;
import io.reactivex.Single;
import java.util.List;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIAssistantMock(OpenAiApi openAiApi) {

  public void mockThreadCompletion(Object result, String runId) {
    mockCreateRunInProcess(runId);
    Run retrievedRun = getRunThatCompleted(runId, result);
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  public void mockSubmitOutputAndCompletion(Object result, String runId) {
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    Run run = getRunThatCompleted(runId, result);
    when(openAiApi.submitToolOutputs(any(), any(), any())).thenReturn(Single.just(run));
  }

  public void mockSubmitOutputAndRequiredMoreAction(Object result, String runId) {
    Run run =
        getRunThatRequiresAction(
            new ObjectMapper().valueToTree(result).toString(), runId, askClarificationQuestion);
    when(openAiApi.submitToolOutputs(any(), any(), any())).thenReturn(Single.just(run));
  }

  private static Run getRunThatCompleted(String runId, Object result) {
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    return getRunThatRequiresAction(arguments.toString(), runId, COMPLETE_NOTE_DETAILS);
  }

  private void mockCreateRunInProcess(String runId) {
    Run run = new Run();
    run.setId(runId);
    run.setStatus("processing");
    Mockito.doReturn(Single.just(run))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private static Run getRunThatRequiresAction(
      String arguments, String runId, String function_name) {
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
                                        .name(function_name)
                                        .arguments(arguments)
                                        .build())
                                .build()))
                    .build())
            .build());
    return retrievedRun;
  }
}
