package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;

public final class OpenAiRunRequiredAction extends OpenAiRun {
  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public OpenAiRunRequiredAction(OpenAiApiHandler openAiApiHandler, Run updatedRun, AiTool tool) {
    super(openAiApiHandler, updatedRun, tool);
  }

  @Override
  public Object getFirstArgument() throws JsonProcessingException {
    return objectMapper.readValue(
        getFirstToolCall().getFunction().getArguments().toString(), tool.parameterClass());
  }

  @Override
  public ToolCallInfo getToolCallInfo() {
    ToolCallInfo toolCallInfo = new ToolCallInfo();
    toolCallInfo.setThreadId(run.getThreadId());
    toolCallInfo.setRunId(run.getId());
    toolCallInfo.setToolCallId(getFirstToolCall().getId());
    return toolCallInfo;
  }

  private ToolCall getFirstToolCall() {
    RequiredAction requiredAction = run.getRequiredAction();
    int size = requiredAction.getSubmitToolOutputs().getToolCalls().size();
    if (size != 1) {
      throw new RuntimeException("Unexpected number of tool calls: " + size);
    }
    return requiredAction.getSubmitToolOutputs().getToolCalls().getFirst();
  }
}
