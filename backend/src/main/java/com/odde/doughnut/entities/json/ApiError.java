package com.odde.doughnut.entities.json;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public class ApiError {

  @Getter private final String message;
  @Getter private final Map<String, String> errors;

  @Getter private final ErrorType errorType;

  public enum ErrorType {
    OPENAI_UNAUTHORIZED,
    BINDING_ERROR,
    OPENAI_TIMEOUT,
  };

  public ApiError(String message, ErrorType type) {
    this.errorType = type;
    this.message = message;
    this.errors = new HashMap<>();
  }

  public void add(String field, String message) {
    errors.put(field, message);
  }
}
