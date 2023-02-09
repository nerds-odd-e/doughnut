package com.odde.doughnut.exceptions;

public class OpenAiUnauthorizedException extends RuntimeException {
  public OpenAiUnauthorizedException(String message) {
    super(message);
  }
}
