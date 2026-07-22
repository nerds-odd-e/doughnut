package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthFindingItem {

  @Schema(description = "Target folder id when the finding refers to a folder")
  private Integer folderId;

  @Schema(description = "Target note id when the finding refers to a note")
  private Integer noteId;

  @Schema(description = "Short display label for the finding target")
  private String label;

  @Schema(description = "Optional human-readable detail message")
  private String message;

  @Schema(description = "Optional wiki-link token for dead-link findings")
  private String wikiLinkToken;
}
