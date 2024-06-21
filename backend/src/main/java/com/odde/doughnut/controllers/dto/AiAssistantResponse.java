package com.odde.doughnut.controllers.dto;

import com.theokanning.openai.assistants.message.Message;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiAssistantResponse {
  String threadId;
  String runId;
  AiCompletionRequiredAction requiredAction;
  List<Message> messages;
}
