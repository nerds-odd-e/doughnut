package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class QuizQuestionCreationParams {
  public int noteId;
  public int correctAnswerIndex;
  public MultipleChoicesQuestion multipleChoicesQuestion;
}
