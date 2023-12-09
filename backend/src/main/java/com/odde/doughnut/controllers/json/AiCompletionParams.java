package com.odde.doughnut.controllers.json;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.services.ai.ClarifyingQuestion;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class AiCompletionParams {
  @Setter private String detailsToComplete;

  @Getter
  private List<ClarifyingQuestionAndAnswer> clarifyingQuestionAndAnswers = new ArrayList<>();

  public String getDetailsToComplete() {
    if (detailsToComplete == null) {
      return "";
    }
    return detailsToComplete;
  }

  public String complete(JsonNode jsonNode) {
    try {
      NoteDetailsCompletion noteDetailsCompletion =
          defaultObjectMapper().treeToValue(jsonNode, NoteDetailsCompletion.class);
      return detailsToComplete = getDetailsToComplete() + noteDetailsCompletion.completion;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String clarifyingQuestion(JsonNode jsonNode) {
    try {
      return defaultObjectMapper().treeToValue(jsonNode, ClarifyingQuestion.class).question;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
