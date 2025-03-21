package com.odde.doughnut.configs;

import com.odde.doughnut.controllers.dto.ApiError;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice()
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      final Exception ex,
      final Object body,
      final @NotNull HttpHeaders headers,
      final @NotNull HttpStatusCode status,
      final @NotNull WebRequest request) {
    return super.handleExceptionInternal(ex, body, headers, status, request);
  }

  @ExceptionHandler({BindException.class})
  public ResponseEntity<Object> handleBindException(
      final BindException ex, final WebRequest request) {
    final ApiError apiError = new ApiError("binding error", ApiError.ErrorType.BINDING_ERROR);
    for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
      apiError.add(error.getField(), error.getDefaultMessage());
    }
    for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
      apiError.add(error.getObjectName(), error.getDefaultMessage());
    }
    return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      @NotNull HttpHeaders headers,
      @NotNull HttpStatusCode status,
      @NotNull WebRequest request) {
    final ApiError apiError = new ApiError("binding error", ApiError.ErrorType.BINDING_ERROR);
    for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
      apiError.add(error.getField(), error.getDefaultMessage());
    }
    for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
      apiError.add(error.getObjectName(), error.getDefaultMessage());
    }
    return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }
}
