package com.odde.donut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class NoteTrashDTO {
  @Getter @Setter @NotNull private NoteTrashReferenceHandling referenceHandling;

  /**
   * Human-readable property key (relation label) when {@link
   * NoteTrashReferenceHandling#REDUCE_TO_SOURCE_PROPERTY}; computed by the client.
   */
  @Getter @Setter private String sourcePropertyKey;
}
