package com.odde.donut.services.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.Mcq;
import com.odde.donut.services.ai.GeneratedMcq;
import java.util.ArrayList;
import java.util.List;

final class RegenerateQuestionMessage {
  private RegenerateQuestionMessage() {}

  static String build(QuestionContestResult contestResult, Mcq mcq) {
    String mcqJson;
    try {
      mcqJson =
          new ObjectMapperConfig()
              .objectMapper()
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(generatedMcqView(mcq));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return """
                Previously generated non-feasible question:

                %s

                Improvement advice:

                %s

                Please regenerate or refine the question based on the above advice."""
        .formatted(mcqJson, contestResult.advice);
  }

  private static GeneratedMcq generatedMcqView(Mcq mcq) {
    List<String> choices = mcq.getResponseChoices();
    int correctIndex = mcq.getCorrectAnswerIndex();
    List<String> distractors = new ArrayList<>(choices);
    distractors.remove(correctIndex);
    return new GeneratedMcq(
        mcq.getQuestionStem(),
        choices.get(correctIndex),
        distractors,
        mcq.getTestedFocus(),
        mcq.getValidationRationale());
  }
}
