package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

class ControllerSetupMultipartExceptionTest {

  ControllerSetup controllerSetup;

  @BeforeEach
  void setup() {
    controllerSetup =
        new ControllerSetup(
            mock(FailureReportRepository.class),
            mock(CurrentUserFetcher.class),
            mock(TestabilitySettings.class));
  }

  @Test
  void returnsPayloadTooLargeForMaxUploadSizeExceeded() {
    ResponseEntity<ApiError> res =
        controllerSetup.handleMultipartException(new MaxUploadSizeExceededException(1L));
    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, res.getStatusCode());
    ApiError body = res.getBody();
    assertNotNull(body);
    assertThat(body.getErrorType(), equalTo(ApiError.ErrorType.MULTIPART_SIZE_EXCEEDED));
    assertThat(body.getMessage(), containsString("100 MB"));
  }

  @Test
  void returnsBadRequestApiErrorForOtherMultipartFailures() {
    ResponseEntity<ApiError> res =
        controllerSetup.handleMultipartException(new MultipartException("boundary missing"));
    assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    ApiError body = res.getBody();
    assertNotNull(body);
    assertThat(body.getErrorType(), equalTo(ApiError.ErrorType.MULTIPART_ERROR));
  }
}
