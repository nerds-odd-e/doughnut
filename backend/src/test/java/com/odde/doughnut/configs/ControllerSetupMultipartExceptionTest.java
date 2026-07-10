package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.odde.doughnut.controllers.dto.ApiError;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

class ControllerSetupMultipartExceptionTest {

  private final ControllerSetup controllerSetup = new ControllerSetup(null, null, null);

  static Stream<Arguments> multipartExceptions() {
    return Stream.of(
        Arguments.of(
            new MaxUploadSizeExceededException(1L),
            HttpStatus.PAYLOAD_TOO_LARGE,
            ApiError.ErrorType.MULTIPART_SIZE_EXCEEDED,
            "100 MB"),
        Arguments.of(
            new MultipartException("boundary missing"),
            HttpStatus.BAD_REQUEST,
            ApiError.ErrorType.MULTIPART_ERROR,
            null));
  }

  @ParameterizedTest
  @MethodSource("multipartExceptions")
  void mapsMultipartExceptionToApiError(
      MultipartException exception,
      HttpStatus expectedStatus,
      ApiError.ErrorType expectedErrorType,
      String expectedMessageFragment) {
    ResponseEntity<ApiError> res = controllerSetup.handleMultipartException(exception);
    assertEquals(expectedStatus, res.getStatusCode());
    ApiError body = res.getBody();
    assertNotNull(body);
    assertThat(body.getErrorType(), equalTo(expectedErrorType));
    if (expectedMessageFragment != null) {
      assertThat(body.getMessage(), containsString(expectedMessageFragment));
    }
  }
}
