package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class NoteDeleteDTO {
  @Getter @Setter @NotNull private NoteDeleteReferenceHandling referenceHandling;
}
