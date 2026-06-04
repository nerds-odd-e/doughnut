package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class NoteDeleteDTO {
  @Getter @Setter @NotNull private NoteDeleteReferenceHandling referenceHandling;

  /**
   * Human-readable property key (relation label) when {@link
   * NoteDeleteReferenceHandling#REDUCE_TO_SOURCE_PROPERTY}; computed by the client.
   */
  @Getter @Setter private String sourcePropertyKey;
}
