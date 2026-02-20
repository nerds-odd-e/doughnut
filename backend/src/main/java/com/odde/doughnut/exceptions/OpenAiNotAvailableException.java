package com.odde.doughnut.exceptions;

import com.odde.doughnut.controllers.dto.ApiError;

public class OpenAiNotAvailableException extends ApiException {
  public OpenAiNotAvailableException(String message) {
    super(
        message,
        ApiError.ErrorType.OPENAI_NOT_AVAILABLE,
        "OpenAI is not available (no API key configured).");
  }
}
