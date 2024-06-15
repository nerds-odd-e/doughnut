package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class NotebookSettings {
  @Getter @Setter Boolean skipReviewEntirely;
  @Getter @Setter Integer numberOfQuestionsInAssessment;
}
