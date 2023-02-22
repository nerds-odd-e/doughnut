package com.odde.doughnut.exceptions;

import com.odde.doughnut.entities.json.ApiError;

public class ApiException extends RuntimeException {
  public ApiException(String message) {
    super(message);
  }

  public ApiError getErrorBody() {
    ApiError apiError = new ApiError("The OpenAI request was not Authorized.");
    apiError.add("_originalMessage", getMessage());
    return apiError;
  }
}
