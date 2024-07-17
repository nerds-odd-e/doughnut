package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionAnswerPair {
  @Setter private Integer questionId;
  @Setter private Integer answerId;
  @Setter private boolean correctAnswers;
}
