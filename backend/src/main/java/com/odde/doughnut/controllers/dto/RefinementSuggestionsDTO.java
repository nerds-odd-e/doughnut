package com.odde.doughnut.controllers.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefinementSuggestionsDTO {
  private List<String> suggestions;

  public RefinementSuggestionsDTO(List<String> suggestions) {
    this.suggestions = suggestions;
  }
}
