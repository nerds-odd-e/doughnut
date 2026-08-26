package com.odde.donut.exceptions;

import com.odde.donut.controllers.dto.ApiError;
import org.springframework.http.HttpStatus;

public class OpenAIServiceErrorException extends ApiException {
  public OpenAIServiceErrorException(String message, HttpStatus status) {
    super(message, ApiError.ErrorType.OPENAI_SERVICE_ERROR, message);
  }
}
