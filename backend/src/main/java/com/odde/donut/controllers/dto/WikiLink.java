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
  public enum Resolution {
    RESOLVED,
    UNRESOLVED,
    AMBIGUOUS
  }

  /** Full authored link spelling as stored (wiki inner, or Markdown {@code [display](href)}). */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String authoredLink;

  /**
   * Resolution target spelling: wiki Portable path (part before {@code |}), or recognized note URL
   * href (e.g. {@code /n1234}). Not every value is a Portable path.
   */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String target;

  /** Visible label in rich mode (wiki part after {@code |}, or Markdown link display text). */
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String displayText;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Resolution resolution;

  /** Present when resolution is RESOLVED. */
  @Schema(
      description = "Present when resolution is RESOLVED",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Integer destinationNoteId;
}
