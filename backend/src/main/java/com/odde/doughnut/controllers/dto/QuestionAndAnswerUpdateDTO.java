package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.QuizQuestion;
import lombok.Getter;
import lombok.Setter;

public class QuestionAndAnswerUpdateDTO {
  @Getter @Setter private QuizQuestion quizQuestion;
  @Getter @Setter private Integer correctAnswerIndex;
}
