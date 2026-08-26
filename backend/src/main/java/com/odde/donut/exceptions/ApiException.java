package com.odde.donut.exceptions;

import com.odde.donut.controllers.dto.ApiError;

public class ApiException extends RuntimeException {
  private final ApiError apiError;

  public ApiException(String originalMessage, ApiError.ErrorType type, String message) {
    super(originalMessage);
    apiError = new ApiError(message, type);
    apiError.add("_originalMessage", getMessage());
  }

  public ApiException(ApiError apiError) {
    super(apiError.getMessage());
    this.apiError = apiError;
    apiError.add("_originalMessage", getMessage());
  }

  public ApiError getErrorBody() {
    return apiError;
  }
}
