package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfNote;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

public class SearchTerm {
  @Setter private String searchKey = "";

  @Getter @Setter private Boolean allMyNotebooksAndSubscriptions = false;

  @Getter @Setter private Boolean allMyCircles = false;

  @JsonUseIdInsteadOfNote @Nullable public Note note;

  @JsonIgnore
  public String getTrimmedSearchKey() {
    return searchKey.trim();
  }
}
