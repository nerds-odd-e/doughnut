package com.odde.donut.exceptions;

import com.odde.donut.controllers.dto.ApiError;

public class OpenAiUnauthorizedException extends ApiException {
  public OpenAiUnauthorizedException(String message) {
    super(
        message, ApiError.ErrorType.OPENAI_UNAUTHORIZED, "The OpenAI request was not Authorized.");
  }
}
