package com.odde.donut.controllers.dto;

import com.odde.donut.entities.Answer;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.QuestionType;
import com.odde.donut.entities.RecallPrompt;
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

  private Mcq mcq;

  public static RecallPromptHistoryItem from(RecallPrompt recallPrompt) {
    RecallPromptHistoryItem item = new RecallPromptHistoryItem();
    item.setId(recallPrompt.getId());
    item.setQuestionType(recallPrompt.getQuestionType());
    item.setQuestionGeneratedTime(recallPrompt.getCreatedAt());
    item.setIsContested(recallPrompt.getIsContested());
    item.setAnswerTime(recallPrompt.getAnswerTime());
    item.setAnswer(recallPrompt.getAnswer());
    Mcq mcq = recallPrompt.getMcq();
    item.setMcq(recallPrompt.getAnswer() == null && mcq != null ? mcq.withoutSolution() : mcq);
    return item;
  }
}
