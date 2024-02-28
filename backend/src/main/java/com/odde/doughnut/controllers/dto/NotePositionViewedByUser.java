package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class NotePositionViewedByUser {
  @Getter @Setter private Integer noteId;

  @Getter @Setter private Boolean fromBazaar;

  @Getter @Setter private Circle circle;

  @Getter @Setter private List<Note> ancestors = new ArrayList<>();
}
