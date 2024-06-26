package com.odde.doughnut.testability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.assistants.run.*;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;

public record OpenAIAssistantCreatedRunMocker(OpenAiApi openAiApi, String threadId, String runId) {
  public OpenAIAssistantRunCompletedMocker aRunThatCompleted() {
    Run run = getRun("completed");
    return new OpenAIAssistantRunCompletedMocker(openAiApi, threadId, run);
  }

  public OpenAIAssistantRunCompletedMocker aRunThatRequireAction(
      Object result, String function_name) {
    Run run = getRunThatRequiresAction(result, function_name);
    return new OpenAIAssistantRunCompletedMocker(openAiApi, threadId, run);
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
                                        .arguments(new ObjectMapper().valueToTree(result))
                                        .build())
                                .build()))
                    .build())
            .build());
    return retrievedRun;
  }
}
