package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.ApiError;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CustomRestExceptionHandlerDuplicateTitleTest {

  private final CustomRestExceptionHandler handler = new CustomRestExceptionHandler();

  @Test
  void mapsDuplicateNoteTitleConstraintToResourceConflict() {
    DataIntegrityViolationException ex =
        new DataIntegrityViolationException("uk_note_notebook_folder_title");

    ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(response.getBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(
        response.getBody().getMessage(),
        equalTo("A note with this title already exists in this notebook (folder or top level)."));
  }

  @Test
  void mapsHibernateConstraintViolationForDuplicateTitle() {
    ConstraintViolationException ex =
        new ConstraintViolationException("uk_note_notebook_folder_title", null, null);

    ResponseEntity<ApiError> response = handler.handleHibernateConstraintViolation(ex);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(response.getBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
  }

  @Test
  void rethrowsUnrelatedDataIntegrityViolation() {
    DataIntegrityViolationException ex = new DataIntegrityViolationException("other_constraint");

    assertThrows(
        DataIntegrityViolationException.class, () -> handler.handleDataIntegrityViolation(ex));
  }
}
