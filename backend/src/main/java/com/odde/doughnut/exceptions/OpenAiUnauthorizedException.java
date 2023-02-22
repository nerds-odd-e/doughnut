package com.odde.doughnut.exceptions;

public class OpenAiUnauthorizedException extends ApiException {
  public OpenAiUnauthorizedException(String message) {
    super(message);
  }
}
