package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.RealGithubService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ControllerSetupTest {
  @Autowired FailureReportRepository failureReportRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Mock RealGithubService githubService;
  MockHttpServletRequest request = new MockHttpServletRequest();
  @Mock TestabilitySettings testabilitySettings;

  ControllerSetup controllerSetup;
  CurrentUserFetcherFromRequest currentUserFetcher;

  @BeforeEach
  void setup() throws IOException, InterruptedException {
    when(testabilitySettings.getGithubService()).thenReturn(githubService);
    doReturn(null).when(githubService).createGithubIssue(any());
    currentUserFetcher =
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
      assertThat(failureReport.getErrorDetail(), containsString("HTTP 401"));
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

  @Test
  void shouldHandleOpenAIUnauthorizedException() {
    OpenAiUnauthorizedException exception = new OpenAiUnauthorizedException("Unauthorized");
    ResponseEntity<ApiError> response =
        controllerSetup.handleOpenAIUnauthorizedException(exception);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    ApiError body = response.getBody();
    assertNotNull(body);
    assertThat(body.getErrors().keySet(), contains("_originalMessage"));
    assertThat(body.getErrorType(), equalTo(ApiError.ErrorType.OPENAI_UNAUTHORIZED));
  }

  @Test
  void shouldHandleOpenAITimeoutException() {
    OpenAITimeoutException exception = new OpenAITimeoutException("timeout");
    ResponseEntity<ApiError> response = controllerSetup.handleApiException(exception);
    assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    ApiError body = response.getBody();
    assertNotNull(body);
    assertThat(body.getErrors().keySet(), contains("_originalMessage"));
    assertThat(body.getErrorType(), equalTo(ApiError.ErrorType.OPENAI_TIMEOUT));
  }

  private FailureReport catchExceptionAndGetFailureReport() {
    assertThrows(
        RuntimeException.class,
        () -> controllerSetup.handleSystemException(request, new RuntimeException()));
    return failureReportRepository.findAll().iterator().next();
  }
}
