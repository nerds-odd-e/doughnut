package com.odde.doughnut.exceptions;

import com.odde.doughnut.controllers.json.ApiError;

public class OpenAITimeoutException extends ApiException {
  public OpenAITimeoutException(String message) {
    super(message, ApiError.ErrorType.OPENAI_TIMEOUT, "The OpenAI request timed out.");
  }
}
