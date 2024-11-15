package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.run.ToolCall;
import java.util.List;
import lombok.Data;

@Data
public final class AiAssistantResponse {
  private final AiTool tool;
  List<Message> messages;
  List<ToolCall> toolCalls;

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public AiAssistantResponse(AiTool tool) {
    this.tool = tool;
  }

  public Object getFirstArgument() throws JsonProcessingException {
    return objectMapper.readValue(
        this.toolCalls.getFirst().getFunction().getArguments().toString(), tool.parameterClass());
  }
}
