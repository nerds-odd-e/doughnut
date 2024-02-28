package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class SearchTerm {
  @NotNull @Getter @Setter private String searchKey = "";

  @Getter @Setter private Boolean allMyNotebooksAndSubscriptions = false;

  @Getter @Setter private Boolean allMyCircles = false;

  @JsonIgnore
  public String getTrimmedSearchKey() {
    return searchKey.trim();
  }
}
