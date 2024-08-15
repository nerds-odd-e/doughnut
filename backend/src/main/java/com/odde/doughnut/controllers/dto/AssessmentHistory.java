package com.odde.doughnut.controllers.dto;

import java.sql.Timestamp;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentHistory {
  public int id;
  public String notebookTitle;
  public Timestamp submittedAt;
  public String result;
}
