package com.odde.doughnut.entities.json;

import com.odde.doughnut.algorithms.ClozePatternCreator;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
@AllArgsConstructor
public final class AiCompletion {
  String suggestion;
  String finishReason;

  public static AiCompletion from(ChatCompletionChoice chatCompletionChoice) {
    return new AiCompletion(
        chatCompletionChoice.getMessage().getContent(), chatCompletionChoice.getFinishReason());
  }

  public AiCompletion prependPreviousIncompleteMessage(String incompleteAssistantMessage) {
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
