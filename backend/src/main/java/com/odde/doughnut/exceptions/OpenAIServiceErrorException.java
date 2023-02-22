package com.odde.doughnut.exceptions;

import com.odde.doughnut.entities.json.ApiError;
import org.springframework.http.HttpStatus;

public class OpenAIServiceErrorException extends ApiException {
  public OpenAIServiceErrorException(String originalMessage, HttpStatus status) {
    super(
        originalMessage,
        ApiError.ErrorType.OPENAI_SERVICE_ERROR,
        "Open AI service error, with status: " + status.name());
  }
}
