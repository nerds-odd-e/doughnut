package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.book.ContentLocator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookLastReadPositionRequest {

  @Schema(
      nullable = true,
      description =
          "Reading position as EpubLocator_Full or PdfLocator_Full. Required when creating a"
              + " stored position or changing scroll location; omit when only updating"
              + " selectedBookBlockId and a locator is already stored.")
  private ContentLocator locator;

  @Schema(
      nullable = true,
      description =
          "Selected book block id for reading UI; omit or null to leave the stored value unchanged")
  private Integer selectedBookBlockId;
}
