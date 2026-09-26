package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.ApiError;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class CustomRestExceptionHandlerBindingErrorTest {

  private final CustomRestExceptionHandler handler = new CustomRestExceptionHandler();

  private static BindingResult bindingResultWith(FieldError... errors) {
    BindingResult result = new MapBindingResult(Map.of(), "form");
    for (FieldError error : errors) {
      result.addError(error);
    }
    return result;
  }

  @Test
  void bindExceptionMessageIsTheFieldMessage() {
    BindException ex =
        new BindException(
            bindingResultWith(new FieldError("form", "uploadPicture", "Picture is too large")));

    ResponseEntity<Object> response = handler.handleBindException(ex, null);

    ApiError body = (ApiError) response.getBody();
    assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(body.getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(body.getMessage(), equalTo("Picture is too large"));
    assertThat(body.getErrors(), equalTo(Map.of("uploadPicture", "Picture is too large")));
  }

  @Test
  void methodArgumentNotValidJoinsSeveralFieldMessages() {
    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(
            null,
            bindingResultWith(
                new FieldError("form", "newTitle", "Title is reserved"),
                new FieldError("form", "details", "Details are too long")));

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, null);

    ApiError body = (ApiError) response.getBody();
    assertThat(body.getMessage(), equalTo("Title is reserved; Details are too long"));
    assertThat(
        body.getErrors(),
        equalTo(Map.of("newTitle", "Title is reserved", "details", "Details are too long")));
  }
}
