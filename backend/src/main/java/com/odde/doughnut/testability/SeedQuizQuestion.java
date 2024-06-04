package com.odde.doughnut.testability;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SeedQuizQuestion {
  private List<Map<String, String>> seedQuizQuestions;
  private String externalIdentifier;
  private String circleName;
}
