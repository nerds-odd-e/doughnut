package com.odde.donut.services.book;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.BookUserLastReadPosition;
import com.odde.donut.exceptions.ApiException;

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
