package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

public class SearchTerm {
  @Setter private String searchKey = "";

  @Getter @Setter private Boolean allMyNotebooksAndSubscriptions = false;

  @Getter @Setter private Boolean allMyCircles = false;

  @JsonIgnore
  public String getTrimmedSearchKey() {
    return searchKey.trim();
  }
}
