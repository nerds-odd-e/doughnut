package com.odde.doughnut.entities.json;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
public final class AiSuggestion {
  String suggestion;
  String finishReason;

  public AiSuggestion(ChatCompletionChoice chatCompletionChoice) {
    this.suggestion = chatCompletionChoice.getMessage().getContent();
    this.finishReason = chatCompletionChoice.getFinishReason();
  }

  public AiSuggestion prependPreviousIncompleteMessage(AiSuggestionRequest aiSuggestionRequest) {
    String incompleteAssistantMessage = aiSuggestionRequest.getIncompleteMessageOrEmptyString();
    if (!Strings.isBlank(incompleteAssistantMessage)) {
      incompleteAssistantMessage += " ";
    }
    setSuggestion(incompleteAssistantMessage + getSuggestion());
    return this;
  }
}
