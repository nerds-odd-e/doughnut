package com.odde.doughnut.controllers.json;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AiCompletionParams {
  public String detailsToComplete = "";
  public String questionFromAI = "";
  public String answerFromUser = "";

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
