package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import java.util.List;
import lombok.Getter;

public class OpenAiRun {
  private final OpenAiApiHandler openAiApiHandler;
  @Getter private final String threadId;
  private final Run run;
  private final AiTool tool;

  public OpenAiRun(OpenAiApiHandler openAiApiHandler, String threadId, Run run, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.threadId = threadId;
    this.run = run;
    this.tool = tool;
  }

  public OpenAiRun(OpenAiApiHandler openAiApiHandler, String threadId, String runId, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.threadId = threadId;
    this.tool = tool;
    this.run = new Run();
    this.run.setId(runId);
  }

  public OpenAiRun submitToolOutputs(String toolCallId, Object result)
      throws JsonProcessingException {
    Run currentRun = openAiApiHandler.submitToolOutputs(threadId, run.getId(), toolCallId, result);
    return new OpenAiRun(openAiApiHandler, threadId, currentRun, tool);
  }

  public void cancelRun() {
    openAiApiHandler.cancelRun(threadId, run.getId());
  }

  public AiAssistantResponse getToolCallResponse() {
    String id = run.getId();
    AiAssistantResponse response = new AiAssistantResponse(tool);

    Run updatedRun = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, run);
    response.setRunStatus(updatedRun.getStatus());
    if (updatedRun.getStatus().equals("requires_action")) {
      response.setToolCalls(getAiCompletionRequiredAction(updatedRun.getRequiredAction()));
    } else {
      response.setMessages(openAiApiHandler.getThreadMessages(threadId, id));
    }

    return response;
  }

  private List<ToolCall> getAiCompletionRequiredAction(RequiredAction requiredAction) {
    int size = requiredAction.getSubmitToolOutputs().getToolCalls().size();
    if (size != 1) {
      throw new RuntimeException("Unexpected number of tool calls: " + size);
    }
    return requiredAction.getSubmitToolOutputs().getToolCalls();
  }

  public String getRunId() {
    return run.getId();
  }
}
