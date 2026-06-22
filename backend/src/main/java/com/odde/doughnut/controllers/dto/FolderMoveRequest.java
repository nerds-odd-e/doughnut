package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Move a folder to a new parent in the same notebook, or to another notebook's root when"
            + " destinationNotebookId is set. Omit newParentFolderId or set it to null to place"
            + " the folder at notebook root.")
@Getter
@Setter
public class FolderMoveRequest {

  @Schema(
      description =
          "Target parent folder id. When null or omitted, the folder is moved to notebook root.")
  private Integer newParentFolderId;

  @Schema(
      description =
          "Destination notebook id for a cross-notebook move to that notebook's root. When null"
              + " or omitted, the folder stays in the path notebook.")
  private Integer destinationNotebookId;

  @Schema(
      description =
          "When true, merges the folder into an existing same-name sibling at the destination"
              + " instead of returning 409. Subfolder name clashes are resolved recursively.")
  private boolean merge = false;
}
