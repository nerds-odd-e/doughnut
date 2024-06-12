package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class NotebookDTO {
  @Getter @Setter Boolean skipReviewEntirely = false;
  @Getter @Setter Integer numberOfQuestions;
}
