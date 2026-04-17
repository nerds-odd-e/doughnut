package com.odde.doughnut.services.book;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.exceptions.ApiException;

public final class ReadingPositionLocatorResolver {

  private ReadingPositionLocatorResolver() {}

  public static ContentLocator resolve(BookUserLastReadPosition row, ObjectMapper objectMapper) {
    String json = row.getReadingPositionLocatorJson();
    if (json == null || json.isBlank()) {
      throw new ApiException(
          "stored reading position has no locator JSON",
          ApiError.ErrorType.BINDING_ERROR,
          "stored reading position has no locator JSON");
    }
    try {
      return objectMapper.readValue(json, ContentLocator.class);
    } catch (JsonProcessingException e) {
      throw new ApiException(
          "stored reading position locator JSON is invalid",
          ApiError.ErrorType.BINDING_ERROR,
          "stored reading position locator JSON is invalid");
    }
  }
}
