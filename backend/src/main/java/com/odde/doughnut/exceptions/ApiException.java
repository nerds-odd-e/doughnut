package com.odde.doughnut.exceptions;

import com.odde.doughnut.controllers.json.ApiError;

public class ApiException extends RuntimeException {
  private final ApiError apiError;

  public ApiException(String originalMessage, ApiError.ErrorType type, String message) {
    super(originalMessage);
    apiError = new ApiError(message, type);
    apiError.add("_originalMessage", getMessage());
  }

  public ApiError getErrorBody() {
    return apiError;
  }
}
