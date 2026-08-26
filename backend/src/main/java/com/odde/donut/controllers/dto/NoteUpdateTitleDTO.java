package com.odde.donut.controllers.dto;

import com.odde.donut.entities.Note;
import com.odde.donut.validators.DisplayNamePathSeparators;
import com.odde.donut.validators.NotBlankDisplayName;
import com.odde.donut.validators.NotReservedNoteTitle;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class NoteUpdateTitleDTO {
  @NotBlankDisplayName
  @Size(max = Note.MAX_TITLE_LENGTH)
  @Pattern(regexp = DisplayNamePathSeparators.REGEXP, message = DisplayNamePathSeparators.MESSAGE)
  @NotReservedNoteTitle
  @Getter
  @Setter
  private String newTitle = "";

  @Getter @Setter private TitleRenameReferenceHandling referenceHandling;
}
