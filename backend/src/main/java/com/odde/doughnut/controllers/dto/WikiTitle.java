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
  /** Full wiki link inner text as stored in markdown (between {@code [[} and {@code ]]}}). */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String linkText;

  /** Target token used for resolution (part before {@code |}). */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String targetToken;

  /** Visible label in rich mode (part after {@code |}, or same as target when absent). */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String displayText;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer noteId;
}
