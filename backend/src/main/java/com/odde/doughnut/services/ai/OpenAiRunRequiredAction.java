package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;

public final class OpenAiRunRequiredAction extends OpenAiOngoingRun implements OpenAiRunResult {
  private final ObjectMapper objectMapper =
      new com.odde.doughnut.configs.ObjectMapperConfig()
          .objectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public OpenAiRunRequiredAction(OpenAiApiHandler openAiApiHandler, Run updatedRun, AiTool tool) {
    super(openAiApiHandler, updatedRun, tool);
  }

  public Object getTheOnlyArgument() throws JsonProcessingException {
    return objectMapper.readValue(
        getTheOnlyToolCall().getFunction().getArguments().toString(), tool.parameterClass());
  }

  public Object getLastArgument() throws JsonProcessingException {
    return objectMapper.readValue(
        getLastToolCall().getFunction().getArguments().toString(), tool.parameterClass());
  }

  public ToolCall getLastToolCall() {
    RequiredAction requiredAction = run.getRequiredAction();
    var toolCalls = requiredAction.getSubmitToolOutputs().getToolCalls();
    return toolCalls.get(toolCalls.size() - 1);
  }

  private ToolCall getTheOnlyToolCall() {
    RequiredAction requiredAction = run.getRequiredAction();
    int size = requiredAction.getSubmitToolOutputs().getToolCalls().size();
    if (size != 1) {
      throw new RuntimeException("Unexpected number of tool calls: " + size);
    }
    return requiredAction.getSubmitToolOutputs().getToolCalls().get(0);
  }
}
