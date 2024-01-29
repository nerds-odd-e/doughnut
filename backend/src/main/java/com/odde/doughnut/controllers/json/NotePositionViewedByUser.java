package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class NotePositionViewedByUser {
  @Getter @Setter private Integer noteId;

  @Getter
  @Setter
  @JsonIgnoreProperties({"headNote"})
  private NotebookViewedByUser notebook;

  @Getter @Setter private List<Note> ancestors = new ArrayList<>();
}
