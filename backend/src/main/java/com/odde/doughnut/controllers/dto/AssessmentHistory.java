package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssessmentHistory {

  public String notebookTitle;
  public Timestamp submittedAt;
  public int answersCorrect;
  public int answersTotal;
}
