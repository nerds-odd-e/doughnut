package com.odde.doughnut.controllers.dto;

import lombok.Getter;

public class QuizQuestionContestResult {
  @Getter public String reason;
  @Getter public Boolean rejected = false;
}
