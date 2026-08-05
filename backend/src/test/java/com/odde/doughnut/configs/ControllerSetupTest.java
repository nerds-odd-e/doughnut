package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ControllerSetupTest {
  @Autowired FailureReportRepository failureReportRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Autowired TestabilitySettings testabilitySettings;
  @MockitoBean GithubService githubService;

  MockHttpServletRequest request = new MockHttpServletRequest();
  ControllerSetup controllerSetup;

  @BeforeEach
  void setup() throws IOException, InterruptedException {
    testabilitySettings.setUseRealGithub(true);
    doReturn(null).when(githubService).createGithubIssue(any());
    CurrentUserFetcherFromRequest currentUserFetcher =
        new CurrentUserFetcherFromRequest(request, userRepository, userService, Optional.empty());
    controllerSetup =
        new ControllerSetup(failureReportRepository, currentUserFetcher, testabilitySettings);
  }

  @ParameterizedTest
  @MethodSource("githubIssueCreationOutcomes")
  void recordsGithubIssueCreationOutcome(
      boolean githubSucceeds, Integer expectedIssueNumber, String expectedErrorFragment)
      throws IOException, InterruptedException {
    if (githubSucceeds) {
      when(githubService.createGithubIssue(any())).thenReturn(123);
    } else {
      when(githubService.createGithubIssue(any()))
          .thenThrow(
              new IOException("GitHub API returned HTTP 401: {\"message\":\"Bad credentials\"}"));
    }
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertEquals(expectedIssueNumber, failureReport.getIssueNumber());
    if (expectedErrorFragment != null) {
      assertThat(failureReport.getErrorDetail(), containsString(expectedErrorFragment));
    }
  }

  static Stream<Arguments> githubIssueCreationOutcomes() {
    return Stream.of(
        Arguments.of(true, 123, null), Arguments.of(false, null, "GitHub issue creation failed"));
  }

  @Test
  void shouldRecordRequestInfo() {
    request.setRequestURI("/path");
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertThat(failureReport.getErrorDetail(), containsString("/path"));
  }

  @ParameterizedTest
  @MethodSource("exceptionsNotRecorded")
  void shouldNotRecordExcludedExceptions(
      Exception exception, Class<? extends Throwable> expectedType) {
    assertThrows(expectedType, () -> controllerSetup.handleSystemException(request, exception));
    assertThat(failureReportRepository.findAll(), emptyIterable());
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

  @Test
  void shouldHandleOpenAIUnauthorizedException() {
    ResponseEntity<ApiError> response =
        controllerSetup.handleOpenAIUnauthorizedException(
            new OpenAiUnauthorizedException("Unauthorized"));
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertThat(response.getBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_UNAUTHORIZED));
  }

  @Test
  void shouldHandleOpenAITimeoutException() {
    ResponseEntity<ApiError> response =
        controllerSetup.handleApiException(new OpenAITimeoutException("timeout"));
    assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    assertThat(response.getBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_TIMEOUT));
  }

  private FailureReport catchExceptionAndGetFailureReport() {
    assertThrows(
        RuntimeException.class,
        () -> controllerSetup.handleSystemException(request, new RuntimeException()));
    return failureReportRepository.findAll().iterator().next();
  }
}
