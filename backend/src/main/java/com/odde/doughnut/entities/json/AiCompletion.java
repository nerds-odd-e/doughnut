package com.odde.doughnut.entities.json;

import com.odde.doughnut.algorithms.ClozePatternCreator;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
@AllArgsConstructor
public final class AiCompletion {
  String moreCompleteContent;
  String finishReason;

  public static AiCompletion from(ChatCompletionChoice chatCompletionChoice) {
    return new AiCompletion(
        chatCompletionChoice.getMessage().getContent(), chatCompletionChoice.getFinishReason());
  }

  public AiCompletion prependPreviousIncompleteContent(String incompleteContent) {
    if (!Strings.isBlank(incompleteContent)) {
      if (startWithCharacterFromSpaceDelimitedLanguage(moreCompleteContent)) {
        incompleteContent += " ";
      }
    } else {
      incompleteContent = "";
    }
    setMoreCompleteContent(incompleteContent + moreCompleteContent);
    return this;
  }

  private static boolean startWithCharacterFromSpaceDelimitedLanguage(String suggestion1) {
    return suggestion1.matches("^\\b(?![" + ClozePatternCreator.regexForCJK + "]).*");
  }
}
