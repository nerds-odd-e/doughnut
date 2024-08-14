package com.odde.doughnut.controllers.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentHistory {

  public String notebookTitle;
  public Timestamp submittedAt;
  public String result;
}
