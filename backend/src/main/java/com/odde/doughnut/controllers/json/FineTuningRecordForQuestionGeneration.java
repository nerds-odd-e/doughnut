package com.odde.doughnut.controllers.json;

import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class FineTuningRecordForQuestionGeneration {
  private @Getter List<SimplifiedOpenAIChatMessage> messages;

  public static FineTuningRecordForQuestionGeneration generateTrainingData(
      List<ChatMessage> messages, String rawJsonQuestion) {
    List<SimplifiedOpenAIChatMessage> simplifiedOpenAIChatMessages =
        messages.stream()
            .map(
                chatMessage ->
                    new SimplifiedOpenAIChatMessage(
                        chatMessage.getRole(), chatMessage.getContent()))
            .collect(Collectors.toList());
    simplifiedOpenAIChatMessages.add(
        new SimplifiedOpenAIChatMessage(ChatMessageRole.ASSISTANT.value(), rawJsonQuestion));
    return new FineTuningRecordForQuestionGeneration(simplifiedOpenAIChatMessages);
  }
}
