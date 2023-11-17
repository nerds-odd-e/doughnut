package com.odde.doughnut.controllers.json;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.algorithms.ClozePatternCreator;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
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

  public String complete(JsonNode jsonNode) {
    try {
      NoteDetailsCompletion noteDetailsCompletion =
          defaultObjectMapper().treeToValue(jsonNode, NoteDetailsCompletion.class);
      return detailsToComplete += noteDetailsCompletion.completion;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
