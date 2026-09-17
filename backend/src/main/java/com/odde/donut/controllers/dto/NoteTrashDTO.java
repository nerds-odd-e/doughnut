package com.odde.donut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class NoteTrashDTO {
  @Getter @Setter @NotNull private NoteTrashReferenceHandling referenceHandling;
}
