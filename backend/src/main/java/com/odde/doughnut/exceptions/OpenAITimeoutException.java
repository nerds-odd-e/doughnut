package com.odde.doughnut.exceptions;

import com.odde.doughnut.entities.json.ApiError;

public class OpenAITimeoutException extends ApiException {
  public OpenAITimeoutException(String message) {
    super(message, ApiError.ErrorType.OPENAI_TIMEOUT, "The OpenAI request timed out.");
  }
}
