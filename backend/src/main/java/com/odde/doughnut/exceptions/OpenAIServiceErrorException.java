package com.odde.doughnut.exceptions;

import com.odde.doughnut.controllers.dto.ApiError;
import org.springframework.http.HttpStatus;

public class OpenAIServiceErrorException extends ApiException {
  public OpenAIServiceErrorException(String message, HttpStatus status) {
    super(message, ApiError.ErrorType.OPENAI_SERVICE_ERROR, message);
  }
}
