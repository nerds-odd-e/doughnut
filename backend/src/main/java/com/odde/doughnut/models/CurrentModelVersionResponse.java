package com.odde.doughnut.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentModelVersionResponse {
  String currentQuestionGenerationModelVersion;
  String currentEvaluationModelVersion;
  String currentOthersModelVersion;
}
