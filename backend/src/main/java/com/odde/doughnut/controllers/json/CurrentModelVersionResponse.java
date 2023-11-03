package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder({
  "currentQuestionGenerationModelVersion",
  "currentEvaluationModelVersion",
  "currentOthersModelVersion"
})
public class CurrentModelVersionResponse {
  String currentQuestionGenerationModelVersion;
  String currentEvaluationModelVersion;
  String currentOthersModelVersion;
}
