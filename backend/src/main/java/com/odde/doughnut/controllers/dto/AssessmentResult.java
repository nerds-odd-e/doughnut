package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.AssessmentAttempt;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssessmentResult {
  public int correctCount;
  public int totalCount;
  public int notebookId;
  public boolean isCertified;
  public AssessmentAttempt attempt;
}
