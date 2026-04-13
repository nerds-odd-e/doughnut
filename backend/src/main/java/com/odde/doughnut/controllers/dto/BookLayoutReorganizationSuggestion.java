package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@JsonClassDescription(
    "Suggested nesting depths for book layout blocks. Each block id from the input must appear exactly once.")
@Data
@Schema(description = "AI suggestion for reorganizing book block depths")
public class BookLayoutReorganizationSuggestion {

  @NotNull
  @JsonPropertyDescription(
      "One entry per input block, each with the block id and the suggested depth (0 = root).")
  @JsonProperty(required = true)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private List<BlockDepthSuggestion> blocks;

  @JsonClassDescription("Suggested depth for one book block")
  @Data
  @Schema(description = "Suggested depth for a single block")
  public static class BlockDepthSuggestion {

    @NotNull
    @JsonPropertyDescription("Book block id from the user message")
    @JsonProperty(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @NotNull
    @JsonPropertyDescription(
        "Nesting depth in preorder outline form; root-level blocks are 0. Must form a valid tree with prior blocks.")
    @JsonProperty(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer depth;
  }
}
