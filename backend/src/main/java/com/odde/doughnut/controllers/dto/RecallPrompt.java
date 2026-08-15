package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecallPrompt {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Notebook notebook;

  private MultipleChoicesQuestion multipleChoicesQuestion;

  private SpellingQuestion spellingQuestion;

  public static RecallPrompt from(com.odde.doughnut.entities.RecallPrompt recallPrompt) {
    RecallPrompt prompt = new RecallPrompt();
    prompt.setId(recallPrompt.getId());
    prompt.setNotebook(recallPrompt.getNotebook());
    if (recallPrompt.getQuestionType() == QuestionType.MCQ) {
      prompt.setMultipleChoicesQuestion(recallPrompt.getMultipleChoicesQuestion());
    } else {
      prompt.setSpellingQuestion(recallPrompt.getSpellingQuestion());
    }
    return prompt;
  }
}
