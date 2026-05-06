package com.odde.doughnut.configs;

import com.odde.doughnut.controllers.dto.ApiError;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.web.server.ResponseStatusException;
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

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    if (!isDuplicateNoteTitleInNotebookFolder(ex)) {
      throw ex;
    }
    return duplicateNoteTitleConflictResponse();
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleHibernateConstraintViolation(
      ConstraintViolationException ex) {
    if (!isDuplicateNoteTitleInNotebookFolder(ex)) {
      throw ex;
    }
    return duplicateNoteTitleConflictResponse();
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex) {
    String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
    ApiError apiError = new ApiError(message, errorTypeFor(ex.getStatusCode()));
    return ResponseEntity.status(ex.getStatusCode()).body(apiError);
  }

  private static ApiError.ErrorType errorTypeFor(HttpStatusCode status) {
    if (status.value() == HttpStatus.CONFLICT.value()) {
      return ApiError.ErrorType.RESOURCE_CONFLICT;
    }
    return ApiError.ErrorType.BINDING_ERROR;
  }

  private static ResponseEntity<ApiError> duplicateNoteTitleConflictResponse() {
    String message =
        "A note with this title already exists in this notebook (folder or top level).";
    ApiError apiError = new ApiError(message, ApiError.ErrorType.RESOURCE_CONFLICT);
    apiError.add("newTitle", message);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
  }

  private static boolean isDuplicateNoteTitleInNotebookFolder(Throwable ex) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      String msg = t.getMessage();
      if (msg != null && msg.contains("uk_note_notebook_folder_title")) {
        return true;
      }
    }
    return false;
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
