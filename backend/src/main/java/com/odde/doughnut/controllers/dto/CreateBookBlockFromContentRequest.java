package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.BookBlockTitleLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class CreateBookBlockFromContentRequest {

  @NotNull
  @Getter
  @Setter
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "Split the owning book block at this imported content row; that row and following rows become a new child block.")
  private Integer fromBookContentBlockId;

  @Getter
  @Setter
  @Size(max = BookBlockTitleLimits.STRUCTURAL_MAX_CHARS)
  @Schema(
      description =
          "When set, used as the new block structural title (trimmed, max length enforced). Otherwise the title is derived from the first moved content block.")
  private String structuralTitle;
}
