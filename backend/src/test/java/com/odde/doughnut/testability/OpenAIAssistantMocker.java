package com.odde.doughnut.testability;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.assistants.run.*;
import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import java.util.List;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIAssistantMocker(OpenAiApi openAiApi) {
  public OpenAIAssistantThreadMocker mockThreadCreation(String threadId) {
    Thread item = new Thread();
    item.setId(threadId);
    when(openAiApi.createThread(ArgumentMatchers.any())).thenReturn(Single.just(item));
    return new OpenAIAssistantThreadMocker(openAiApi, threadId);
  }

  public void mockThreadRunRequireActionAndCompletionToolCalled(Object result, String runId) {
    Run retrievedRun = getRunThatCallCompletionTool(runId, result);
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  public void mockSubmitOutputAndCompletion(Object result, String runId) {
    Run run = getRunThatCallCompletionTool(runId, result);
    when(openAiApi.submitToolOutputs(any(), any(), any())).thenReturn(Single.just(run));
  }

  public void mockSubmitOutputAndRequiredMoreAction(Object result, String runId) {
    Run run =
        getRunThatRequiresAction(
            new ObjectMapper().valueToTree(result), runId, askClarificationQuestion);
    when(openAiApi.submitToolOutputs(any(), any(), any())).thenReturn(Single.just(run));
  }

  private static Run getRunThatCallCompletionTool(String runId, Object result) {
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    return getRunThatRequiresAction(arguments, runId, COMPLETE_NOTE_DETAILS);
  }

  private static Run getRunThatRequiresAction(
      JsonNode arguments, String runId, String function_name) {
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
