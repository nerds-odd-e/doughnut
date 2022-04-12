package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfNote;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

public class SearchTerm {
  @Setter private String searchKey = "";

  @Getter @Setter private Boolean allMyNotebooksAndSubscriptions = false;

  @Getter @Setter private Boolean allMyCircles = false;

  @JsonUseIdInsteadOfNote public Optional<Note> note = Optional.empty();

  @JsonIgnore
  public String getTrimmedSearchKey() {
    return searchKey.trim();
  }
}
