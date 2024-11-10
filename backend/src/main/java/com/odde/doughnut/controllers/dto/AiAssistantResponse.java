package com.odde.doughnut.controllers.dto;

import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.run.ToolCall;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiAssistantResponse {
  String threadId;
  String runId;
  List<Message> messages;
  List<ToolCall> toolCalls;
}
