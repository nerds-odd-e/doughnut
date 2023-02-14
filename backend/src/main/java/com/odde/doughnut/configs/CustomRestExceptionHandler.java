package com.odde.doughnut.configs;

import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice()
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {
  public static class ApiError {

    @Getter private final HttpStatus status;
    @Getter private final String message;
    @Getter private final Map<String, String> errors;

    public ApiError(HttpStatus status, String message) {
      this.status = status;
      this.message = message;
      this.errors = new HashMap<>();
    }

    public void add(String field, String message) {
      errors.put(field, message);
    }
  }

  @Override
  protected ResponseEntity<Object> handleBindException(
      final BindException ex,
      final HttpHeaders headers,
      final HttpStatus status,
      final WebRequest request) {
    final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "binding error");
    for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
      apiError.add(error.getField(), error.getDefaultMessage());
    }
    for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
      apiError.add(error.getObjectName(), error.getDefaultMessage());
    }
    return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
  }
}
