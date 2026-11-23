package com.odde.donut.services.book;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.BookUserLastReadPosition;
import com.odde.donut.exceptions.ApiException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public final class ReadingPositionLocatorResolver {

  private ReadingPositionLocatorResolver() {}

  public static ContentLocator resolve(BookUserLastReadPosition row, JsonMapper objectMapper) {
    String json = row.getReadingPositionLocatorJson();
    if (json == null || json.isBlank()) {
      throw new ApiException(
          "stored reading position has no locator JSON",
          ApiError.ErrorType.BINDING_ERROR,
          "stored reading position has no locator JSON");
    }
    try {
      return objectMapper.readValue(json, ContentLocator.class);
    } catch (JacksonException e) {
      throw new ApiException(
          "stored reading position locator JSON is invalid",
          ApiError.ErrorType.BINDING_ERROR,
          "stored reading position locator JSON is invalid");
    }
  }
}
