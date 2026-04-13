package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.services.book.PageBbox;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BookBlockMutationResponse {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final int id;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Nesting depth in the book layout; root-level blocks are 0.")
  private final int depth;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final String title;

  @Schema(
      description =
          "PDF anchors; present only when anchors for this block changed (e.g. cancel merge).")
  private final List<PageBbox> allBboxes;

  public BookBlockMutationResponse(int id, int depth, String title, List<PageBbox> allBboxes) {
    this.id = id;
    this.depth = depth;
    this.title = title;
    this.allBboxes = allBboxes;
  }
}
