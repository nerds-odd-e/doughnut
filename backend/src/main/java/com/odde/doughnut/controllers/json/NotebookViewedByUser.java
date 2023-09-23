package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Ownership;
import javax.persistence.Id;
import lombok.Getter;
import lombok.Setter;

public class NotebookViewedByUser {
  @Id @Getter @Setter private Integer id;

  @Id @Getter @Setter private Integer headNoteId;

  @Id @Getter @Setter private Note headNote;

  @Getter @Setter private Boolean skipReviewEntirely;

  @Getter @Setter private Boolean fromBazaar;

  @Getter @Setter private Ownership ownership;
}
