package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.services.TriConsumer;
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
    openAiApiHandler.submitToolOutputs(threadId, run.getId(), toolCallId, result);
    return this;
  }

  public void cancelRun() {
    openAiApiHandler.cancelRun(threadId, run.getId());
  }

  public AiAssistantResponse getToolCallResponse(
      TriConsumer<OpenAiRun, String, Object> runServiceAction) throws JsonProcessingException {
    AiAssistantResponse threadResponse = getThreadResponse(threadId, run);
    if (runServiceAction != null) {
      Object parsedResponse = threadResponse.getFirstArgument();
      runServiceAction.accept(
          this, threadResponse.getToolCalls().getFirst().getId(), parsedResponse);
    }
    return threadResponse;
  }

  private AiAssistantResponse getThreadResponse(String threadId, Run currentRun) {
    String id = currentRun.getId();
    AiAssistantResponse completionResponse = new AiAssistantResponse(tool);

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

  public String getRunId() {
    return run.getId();
  }
}
