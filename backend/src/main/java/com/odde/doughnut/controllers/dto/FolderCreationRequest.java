package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Create a folder under notebook root, nested under an explicit parent folder, or nested"
            + " under the folder of a context note.")
@Getter
@Setter
public class FolderCreationRequest {

  @NotBlank
  @Size(max = 512)
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Display name for the new folder")
  private String name;

  @Schema(
      description =
          "When set, the new folder is created as a direct child of this folder. Must belong to the"
              + " target notebook. Takes precedence over underNoteId when both are set.")
  private Integer underFolderId;

  @Schema(
      description =
          "When set (and underFolderId is not), the new folder is a child of this note's folder (or"
              + " notebook root when the note has no folder). Must belong to the target notebook.")
  private Integer underNoteId;
}
