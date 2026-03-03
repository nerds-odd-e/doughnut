package com.odde.doughnut.configs;

import static org.assertj.core.api.Assertions.assertThat;

import com.odde.doughnut.controllers.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

class CustomRestExceptionHandlerTest {

  private final CustomRestExceptionHandler handler = new CustomRestExceptionHandler();

  @Test
  void shouldReturn400ForMultipartException() {
    MultipartException ex = new MultipartException("Failed to parse multipart request");

    ResponseEntity<Object> response = handler.handleMultipartException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiError apiError = (ApiError) response.getBody();
    assertThat(apiError).isNotNull();
    assertThat(apiError.getErrorType()).isEqualTo(ApiError.ErrorType.MULTIPART_ERROR);
    assertThat(apiError.getMessage()).isEqualTo("Failed to parse multipart request");
  }

  @Test
  void shouldReturn413ForMaxUploadSizeExceededException() {
    MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10_000_000);

    ResponseEntity<Object> response = handler.handleMultipartException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    ApiError apiError = (ApiError) response.getBody();
    assertThat(apiError).isNotNull();
    assertThat(apiError.getErrorType()).isEqualTo(ApiError.ErrorType.MULTIPART_ERROR);
    assertThat(apiError.getMessage()).isEqualTo("File size exceeds the maximum allowed limit");
  }
}
