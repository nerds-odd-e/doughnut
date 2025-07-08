package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.theokanning.openai.assistants.run.*;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import java.util.List;

public record OpenAIAssistantCreatedRunMocker(OpenAiApi openAiApi, String threadId, String runId) {
  public OpenAIAssistantRunMocker anExpiredRun() {
    Run run = getRun("expired");
    return new OpenAIAssistantRunMocker(openAiApi, threadId, run);
  }

  public OpenAIAssistantRunMocker aCompletedRun() {
    Run run = new Run();
    run.setId(runId);
    run.setThreadId(threadId);
    run.setStatus("completed");
    when(openAiApi.createRun(eq(threadId), any(RunCreateRequest.class)))
        .thenReturn(Single.just(run));
    return new OpenAIAssistantRunMocker(openAiApi, threadId, run);
  }

  public OpenAIAssistantRunMocker aRunThatRequireAction(Object result, String function_name) {
    Run run = getRunThatRequiresAction(result, function_name);
    return new OpenAIAssistantRunMocker(openAiApi, threadId, run);
  }

  public OpenAIAssistantRunMocker aRunWithNoToolCalls() {
    Run retrievedRun = getRun("requires_action");
    retrievedRun.setRequiredAction(
        RequiredAction.builder()
            .submitToolOutputs(SubmitToolOutputs.builder().toolCalls(List.of()).build())
            .build());
    return new OpenAIAssistantRunMocker(openAiApi, threadId, retrievedRun);
  }

  private Run getRun(String status) {
    Run run = new Run();
    run.setId(runId);
    run.setStatus(status);
    run.setThreadId(threadId);
    return run;
  }

  private Run getRunThatRequiresAction(Object result, String function_name) {
    Run retrievedRun = getRun("requires_action");
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
                                        .arguments(
                                            new com.odde.doughnut.configs.ObjectMapperConfig()
                                                .objectMapper()
                                                .valueToTree(result))
                                        .build())
                                .build()))
                    .build())
            .build());
    return retrievedRun;
  }
}
