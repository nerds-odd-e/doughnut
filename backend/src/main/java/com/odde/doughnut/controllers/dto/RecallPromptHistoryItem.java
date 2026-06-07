package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecallPromptHistoryItem {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private QuestionType questionType;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Timestamp questionGeneratedTime;

  private Boolean isContested;

  private Timestamp answerTime;

  private Answer answer;

  private PredefinedQuestion predefinedQuestion;

  private MultipleChoicesQuestion multipleChoicesQuestion;

  public static RecallPromptHistoryItem from(RecallPrompt recallPrompt) {
    RecallPromptHistoryItem item = new RecallPromptHistoryItem();
    item.setId(recallPrompt.getId());
    item.setQuestionType(recallPrompt.getQuestionType());
    item.setQuestionGeneratedTime(recallPrompt.getCreatedAt());
    item.setIsContested(recallPrompt.getIsContested());
    item.setAnswerTime(recallPrompt.getAnswerTime());
    item.setAnswer(recallPrompt.getAnswer());
    item.setPredefinedQuestion(recallPrompt.getPredefinedQuestion());
    item.setMultipleChoicesQuestion(recallPrompt.getMultipleChoicesQuestion());
    return item;
  }
}
