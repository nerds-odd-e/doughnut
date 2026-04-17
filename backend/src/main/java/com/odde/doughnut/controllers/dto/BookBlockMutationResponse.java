package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.services.book.ContentLocator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
          "Locators for this block; present only when geometry changed (e.g. cancel merge).")
  private final List<ContentLocator> contentLocators;

  public BookBlockMutationResponse(
      int id, int depth, String title, List<ContentLocator> contentLocators) {
    this.id = id;
    this.depth = depth;
    this.title = title;
    this.contentLocators = contentLocators;
  }

  public int getId() {
    return id;
  }

  public int getDepth() {
    return depth;
  }

  public String getTitle() {
    return title;
  }

  @JsonView(BookViews.Full.class)
  public List<ContentLocator> getContentLocators() {
    return contentLocators;
  }
}
