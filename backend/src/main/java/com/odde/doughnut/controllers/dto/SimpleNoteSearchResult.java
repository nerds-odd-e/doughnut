package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleNoteSearchResult {
  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer id;

  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer notebookId;

  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String titleOrPredicate;
}
