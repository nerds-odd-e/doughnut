package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.services.TriConsumer;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import java.util.List;

public class OpenAiRun {
  private final OpenAiApiHandler openAiApiHandler;
  private final String threadId;
  private final Run run;
  private final AiTool tool;
  private final ObjectMapper objectMapper;

  public OpenAiRun(OpenAiApiHandler openAiApiHandler, String threadId, Run run, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.threadId = threadId;
    this.run = run;
    this.tool = tool;
    this.objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public void getToolCallResponse(TriConsumer<AssistantRunService, String, Object> runServiceAction)
      throws JsonProcessingException {
    AiAssistantResponse threadResponse = getThreadResponse(threadId, run);
    AssistantRunService runService =
        new AssistantRunService(openAiApiHandler, threadId, threadResponse.getRunId());
    if (runServiceAction != null) {
      Object parsedResponse =
          objectMapper.readValue(
              threadResponse.getToolCalls().getFirst().getFunction().getArguments().toString(),
              tool.parameterClass());
      runServiceAction.accept(
          runService, threadResponse.getToolCalls().getFirst().getId(), parsedResponse);
    }
  }

  private AiAssistantResponse getThreadResponse(String threadId, Run currentRun) {
    String id = currentRun.getId();
    AiAssistantResponse completionResponse = new AiAssistantResponse();
    completionResponse.setThreadId(threadId);
    completionResponse.setRunId(id);

    Run run = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, currentRun);
    if (run.getStatus().equals("requires_action")) {
      completionResponse.setToolCalls(getAiCompletionRequiredAction(run.getRequiredAction()));
    } else {
      completionResponse.setMessages(openAiApiHandler.getThreadMessages(threadId, id));
    }

    return completionResponse;
  }

  private List<ToolCall> getAiCompletionRequiredAction(RequiredAction requiredAction) {
    int size = requiredAction.getSubmitToolOutputs().getToolCalls().size();
    if (size != 1) {
      throw new RuntimeException("Unexpected number of tool calls: " + size);
    }
    return requiredAction.getSubmitToolOutputs().getToolCalls();
  }
}
