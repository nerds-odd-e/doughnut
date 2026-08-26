package com.odde.donut.exceptions;

import com.odde.donut.controllers.dto.ApiError;

public class OpenAiNotAvailableException extends ApiException {
  public OpenAiNotAvailableException(String message) {
    super(
        message,
        ApiError.ErrorType.OPENAI_NOT_AVAILABLE,
        "OpenAI is not available (no API key configured).");
  }
}
