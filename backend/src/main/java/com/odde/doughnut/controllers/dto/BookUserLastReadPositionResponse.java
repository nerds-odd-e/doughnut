package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.services.book.ContentLocator;
import com.odde.doughnut.services.book.ReadingPositionLocatorResolver;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookUserLastReadPosition")
public record BookUserLastReadPositionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ContentLocator locator,
    @Schema(nullable = true) Integer selectedBookBlockId) {

  public static BookUserLastReadPositionResponse from(
      BookUserLastReadPosition row, ObjectMapper objectMapper) {
    ContentLocator locator = ReadingPositionLocatorResolver.resolve(row, objectMapper);
    return new BookUserLastReadPositionResponse(row.getId(), locator, row.getSelectedBookBlockId());
  }
}
