package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachBookRequest {

  @NotBlank
  @Size(max = 512)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String bookName;

  @NotBlank
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      allowableValues = {"pdf", "epub"},
      description = "Book file format")
  private String format;

  @Valid
  @Schema(
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description =
          "Nested book layout. Omit when sending contentList instead. Exactly one of this"
              + " (non-empty roots) or contentList is required for PDF.")
  private AttachBookLayoutRequest layout;

  @Schema(
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description =
          "MinerU content_list array; server builds the book layout. Mutually exclusive with"
              + " non-empty book layout roots.")
  private List<Object> contentList;
}
