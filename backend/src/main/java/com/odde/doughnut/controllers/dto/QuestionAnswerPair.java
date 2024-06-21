package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class QuestionAnswerPair {
  @Getter @Setter private Integer questionId;
  @Getter @Setter private Integer answerId;
}
