package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentHistory {
  @NotNull public int id;
  @NotNull public String notebookTitle;
  @NotNull public Timestamp submittedAt;
  @NotNull public String result;
}
