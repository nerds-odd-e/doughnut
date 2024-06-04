package com.odde.doughnut.testability;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SeedQuizQuestion {
  private List<Map<String, String>> seedQuizQuestions;
  private String externalIdentifier;
  private String circleName;
}
