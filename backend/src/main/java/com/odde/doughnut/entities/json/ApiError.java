package com.odde.doughnut.entities.json;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class ApiError {

  @Getter
  private final String message;
  @Getter
  private final Map<String, String> errors;

  public ApiError(String message) {
    this.message = message;
    this.errors = new HashMap<>();
  }

  public void add(String field, String message) {
    errors.put(field, message);
  }
}
