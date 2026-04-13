package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class BookMutationResponse {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final Integer id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final String bookName;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final String format;

  private final Timestamp createdAt;
  private final Timestamp updatedAt;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "Book blocks in depth-first preorder (parent before descendants, then siblings). "
              + "Order matches ascending layout_sequence in persistence.")
  private final List<BookBlockMutationResponse> blocks;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string")
  private final String notebookId;

  public BookMutationResponse(
      Integer id,
      String bookName,
      String format,
      Timestamp createdAt,
      Timestamp updatedAt,
      List<BookBlockMutationResponse> blocks,
      String notebookId) {
    this.id = id;
    this.bookName = bookName;
    this.format = format;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.blocks = blocks;
    this.notebookId = notebookId;
  }
}
