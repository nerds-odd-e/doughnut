package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class NotebookViewedByUser {
  @Id @Getter @Setter private Integer id;

  @Id @Getter @Setter private Integer headNoteId;

  @NotNull @Id @Getter @Setter private Note headNote;

  @Getter @Setter private Boolean skipReviewEntirely;
}
