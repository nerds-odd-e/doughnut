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
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String format;

  /**
   * Nested book layout. Omit when sending {@link #contentList} instead. Exactly one of this
   * (non-empty roots) or {@code contentList} (non-empty) is required; enforced in {@link
   * com.odde.doughnut.services.book.BookService}.
   */
  @Valid
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private AttachBookLayoutRequest layout;

  /**
   * MinerU {@code content_list} array; server builds {@link #layout}. Mutually exclusive with
   * non-empty {@link AttachBookLayoutRequest#getRoots()}.
   */
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private List<Object> contentList;
}
