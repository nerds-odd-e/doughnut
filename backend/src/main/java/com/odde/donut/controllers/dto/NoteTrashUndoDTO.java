package com.odde.donut.controllers.dto;

import com.odde.donut.entities.Note;
import com.odde.donut.validators.DisplayNamePathSeparators;
import com.odde.donut.validators.NotBlankDisplayName;
import com.odde.donut.validators.NotReservedNoteTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteTrashUndoDTO {
  @NotBlankDisplayName
  @Size(max = Note.MAX_TITLE_LENGTH)
  @Pattern(
      regexp = DisplayNamePathSeparators.NOTE_TITLE_REGEXP,
      message = DisplayNamePathSeparators.NOTE_TITLE_MESSAGE)
  @NotReservedNoteTitle
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String priorTitle;

  @Schema(description = "Prior containing folder id, or null when the note was at notebook root.")
  private Integer priorFolderId;
}
