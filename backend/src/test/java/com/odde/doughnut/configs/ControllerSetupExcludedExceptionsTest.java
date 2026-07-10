package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class ControllerSetupExcludedExceptionsTest {

  private FailureReportRepository failureReportRepository;
  private ControllerSetup controllerSetup;
  private MockHttpServletRequest request;

  @BeforeEach
  void setup() {
    failureReportRepository = mock(FailureReportRepository.class);
    when(failureReportRepository.count()).thenReturn(0L);
    CurrentUserFetcher currentUserFetcher = mock(CurrentUserFetcher.class);
    TestabilitySettings testabilitySettings = mock(TestabilitySettings.class);
    request = new MockHttpServletRequest();
    controllerSetup =
        new ControllerSetup(failureReportRepository, currentUserFetcher, testabilitySettings);
  }

  @ParameterizedTest
  @MethodSource("exceptionsNotRecorded")
  void shouldNotRecordExcludedExceptions(
      Exception exception, Class<? extends Throwable> expectedType) {
    assertThrows(expectedType, () -> controllerSetup.handleSystemException(request, exception));
    assertThat(failureReportRepository.count(), equalTo(0L));
  }

  static Stream<Arguments> exceptionsNotRecorded() {
    return Stream.of(
        Arguments.of(
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "xx"),
            ResponseStatusException.class),
        Arguments.of(
            new ApiException("x", ApiError.ErrorType.BINDING_ERROR, "client error"),
            ApiException.class),
        Arguments.of(
            new UnexpectedNoAccessRightException(), UnexpectedNoAccessRightException.class));
  }
}
