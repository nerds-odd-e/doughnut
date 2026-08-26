package com.odde.donut.exceptions;

import com.odde.donut.controllers.dto.ApiError;

public class OpenAITimeoutException extends ApiException {
  public OpenAITimeoutException(String message) {
    super(message, ApiError.ErrorType.OPENAI_TIMEOUT, "The OpenAI request timed out.");
  }
}
