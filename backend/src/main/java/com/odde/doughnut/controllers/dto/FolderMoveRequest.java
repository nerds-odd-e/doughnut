package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Move a folder to a new parent in the same notebook. Omit newParentFolderId or set it to"
            + " null to place the folder at notebook root.")
@Getter
@Setter
public class FolderMoveRequest {

  @Schema(
      description =
          "Target parent folder id. When null or omitted, the folder is moved to notebook root.")
  private Integer newParentFolderId;
}
