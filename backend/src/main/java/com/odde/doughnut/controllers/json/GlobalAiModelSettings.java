package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"questionGenerationModel", "evaluationModel", "othersModel"})
public class GlobalAiModelSettings {
  String questionGenerationModel;
  String evaluationModel;
  String othersModel;
}
