package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnsweredQuestion {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private QuestionType questionType;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int memoryTrackerId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private RecalledNote recalledNote;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Answer answer;

  private PredefinedQuestion predefinedQuestion;

  public static AnsweredQuestion from(RecallPrompt recallPrompt) {
    Objects.requireNonNull(recallPrompt.getAnswer(), "answered question requires an answer");
    AnsweredQuestion answeredQuestion = new AnsweredQuestion();
    answeredQuestion.setId(recallPrompt.getId());
    answeredQuestion.setQuestionType(recallPrompt.getQuestionType());
    answeredQuestion.setMemoryTrackerId(recallPrompt.requireMemoryTracker().getId());
    answeredQuestion.setRecalledNote(
        RecalledNote.from(recallPrompt.getNote(), recallPrompt.getPropertyKey()));
    answeredQuestion.setAnswer(recallPrompt.getAnswer());
    if (recallPrompt.getQuestionType() == QuestionType.MCQ) {
      answeredQuestion.setPredefinedQuestion(recallPrompt.getPredefinedQuestion());
    }
    return answeredQuestion;
  }
}
