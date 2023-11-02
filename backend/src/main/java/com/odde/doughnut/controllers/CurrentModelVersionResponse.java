package com.odde.doughnut.controllers;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentModelVersionResponse {
  String currentQuestionGenerationModelVersion;
  String currentEvaluationModelVersion;
  String currentOthersModelVersion;
}
