package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import lombok.Setter;

public final class AiAssistantResponse {
  private final AiTool tool;
  private final OpenAiApiHandler openAiApiHandler;
  @Setter Run run;

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public AiAssistantResponse(OpenAiApiHandler openAiApiHandler, Run updatedRun, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.run = updatedRun;
    this.tool = tool;
  }

  public Object getFirstArgument() throws JsonProcessingException {
    return objectMapper.readValue(
        getFirstToolCall().getFunction().getArguments().toString(), tool.parameterClass());
  }

  public String getFirstToolCallId() {
    return getFirstToolCall().getId();
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
