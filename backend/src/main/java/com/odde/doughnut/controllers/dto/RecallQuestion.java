package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecallQuestion {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Notebook notebook;

  private MultipleChoicesQuestion multipleChoicesQuestion;

  private SpellingQuestion spellingQuestion;

  public static RecallQuestion from(RecallPrompt recallPrompt) {
    RecallQuestion question = new RecallQuestion();
    question.setId(recallPrompt.getId());
    question.setNotebook(recallPrompt.getNotebook());
    if (recallPrompt.getQuestionType() == QuestionType.MCQ) {
      question.setMultipleChoicesQuestion(recallPrompt.getMultipleChoicesQuestion());
    } else {
      question.setSpellingQuestion(recallPrompt.getSpellingQuestion());
    }
    return question;
  }
}
