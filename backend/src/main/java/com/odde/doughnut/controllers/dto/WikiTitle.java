package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WikiTitle {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String linkText;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer notebookId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String slug;
}
