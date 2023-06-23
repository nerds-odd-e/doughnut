package com.odde.doughnut.entities.json;

import com.odde.doughnut.algorithms.ClozePatternCreator;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
@AllArgsConstructor
public final class AiSuggestion {
  String suggestion;
  String finishReason;

  public static AiSuggestion from(ChatCompletionChoice chatCompletionChoice) {
    return new AiSuggestion(
        chatCompletionChoice.getMessage().getContent(), chatCompletionChoice.getFinishReason());
  }

  public AiSuggestion prependPreviousIncompleteMessage(String incompleteAssistantMessage) {
    if (!Strings.isBlank(incompleteAssistantMessage)) {
      if (startWithCharacterFromSpaceDelimitedLanguage(suggestion)) {
        incompleteAssistantMessage += " ";
      }
    } else {
      incompleteAssistantMessage = "";
    }
    setSuggestion(incompleteAssistantMessage + suggestion);
    return this;
  }

  private static boolean startWithCharacterFromSpaceDelimitedLanguage(String suggestion1) {
    return suggestion1.matches("^\\b(?![" + ClozePatternCreator.regexForCJK + "]).*");
  }
}
