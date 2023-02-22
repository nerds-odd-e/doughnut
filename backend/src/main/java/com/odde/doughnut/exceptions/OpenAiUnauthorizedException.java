package com.odde.doughnut.exceptions;

import com.odde.doughnut.entities.json.ApiError;

public class OpenAiUnauthorizedException extends ApiException {
  public OpenAiUnauthorizedException(String message) {
    super(
        message, ApiError.ErrorType.OPENAI_UNAUTHORIZED, "The OpenAI request was not Authorized.");
  }
}
