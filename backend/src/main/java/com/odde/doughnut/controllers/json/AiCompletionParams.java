package com.odde.doughnut.controllers.json;

import com.odde.doughnut.algorithms.ClozePatternCreator;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

@NoArgsConstructor
@AllArgsConstructor
public class AiCompletionParams {
  public String detailsToComplete = "";

  public static String concat(String part1, String part2) {
    if (!Strings.isBlank(part1)) {
      if (part2.matches("^\\b(?![" + ClozePatternCreator.regexForCJK + "]).*")) {
        part1 += " ";
      }
    } else {
      part1 = "";
    }
    return part1 + part2;
  }

  public AiCompletion getAiCompletion(ChatCompletionChoice chatCompletionChoice) {
    String appendingContent = chatCompletionChoice.getMessage().getContent();
    return new AiCompletion(
        concat(detailsToComplete, appendingContent), chatCompletionChoice.getFinishReason());
  }
}
