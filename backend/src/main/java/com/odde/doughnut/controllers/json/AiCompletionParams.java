package com.odde.doughnut.controllers.json;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class AiCompletionParams {
  @Setter private String detailsToComplete;

  @Getter
  private List<ClarifyingQuestionAndAnswer> clarifyingQuestionAndAnswers = new ArrayList<>();

  public String getDetailsToComplete() {
    if (detailsToComplete == null) {
      return "";
    }
    return detailsToComplete;
  }
}
