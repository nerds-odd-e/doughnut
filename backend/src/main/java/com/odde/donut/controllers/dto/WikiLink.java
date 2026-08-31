package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WikiLink {
  /** Full wiki link inner text as stored in markdown (between {@code [[} and {@code ]]}}). */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String authoredLink;

  /** Portable path used for resolution (part before {@code |}). */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String portablePath;

  /**
   * Visible label in rich mode (part after {@code |}, or same as the link's Portable path when
   * absent).
   */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String displayText;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer destinationNoteId;
}
