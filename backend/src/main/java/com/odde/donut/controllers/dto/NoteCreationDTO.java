package com.odde.donut.controllers.dto;

import com.odde.donut.validators.DisplayNamePathSeparators;
import com.odde.donut.validators.NotBlankDisplayName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class NoteCreationDTO extends NoteUpdateTitleDTO {
  @Getter @Setter private Integer folderId;

  @Getter @Setter private String content;

  @NotBlankDisplayName(allowNull = true)
  @Size(max = 512)
  @Pattern(regexp = DisplayNamePathSeparators.REGEXP, message = DisplayNamePathSeparators.MESSAGE)
  @Schema(
      description =
          "When set, the note goes into the child folder of this name (ignoring letter case) under"
              + " folderId or the notebook root, created in the same change when absent.")
  @Getter
  @Setter
  private String childFolderName;
}
