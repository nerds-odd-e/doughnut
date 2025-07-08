package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"questionGenerationModel", "evaluationModel", "othersModel"})
public class GlobalAiModelSettings {
  public String questionGenerationModel;
  public String evaluationModel;
  public String othersModel;
}
