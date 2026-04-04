package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.RealGithubService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ControllerSetupTest {
  @Autowired MakeMe makeMe;
  @Autowired FailureReportRepository failureReportRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Mock RealGithubService githubService;
  MockHttpServletRequest request = new MockHttpServletRequest();
  @Mock TestabilitySettings testabilitySettings;

  ControllerSetup controllerSetup;
  CurrentUserFetcherFromRequest currentUserFetcher;

  @BeforeEach
  void setup() {
    when(testabilitySettings.getGithubService()).thenReturn(githubService);
    currentUserFetcher = new CurrentUserFetcherFromRequest(request, userRepository, userService);
    controllerSetup =
        new ControllerSetup(failureReportRepository, currentUserFetcher, testabilitySettings);
  }

  @Test
  void shouldNotRecordResponseStatusException() {
    long count = failureReportRepository.count();
    assertThrows(
        ResponseStatusException.class,
        () ->
            controllerSetup.handleSystemException(
                request, new ResponseStatusException(HttpStatus.UNAUTHORIZED, "xx")));
    assertThat(failureReportRepository.count(), equalTo(count));
  }

  @Test
  void shouldNotRecordApiException() {
    long count = failureReportRepository.count();
    assertThrows(
        ApiException.class,
        () ->
            controllerSetup.handleSystemException(
                request, new ApiException("x", ApiError.ErrorType.BINDING_ERROR, "client error")));
    assertThat(failureReportRepository.count(), equalTo(count));
  }

  @Test
  void shouldNotRecordUnexpectedNoAccessRightException() {
    long count = failureReportRepository.count();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            controllerSetup.handleSystemException(request, new UnexpectedNoAccessRightException()));
    assertThat(failureReportRepository.count(), equalTo(count));
  }

  @Test
  void shouldRecordExceptionDetails() {
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertEquals("java.lang.RuntimeException", failureReport.getErrorName());
    assertThat(failureReport.getErrorDetail(), containsString("ControllerSetupTest.java"));
  }

  @Test
  void shouldCreateGithubIssue() throws IOException, InterruptedException {
    when(githubService.createGithubIssue(any())).thenReturn(123);
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertEquals(123, failureReport.getIssueNumber());
  }

  @Test
  void shouldRecordGithubErrorInFailureReportWhenGithubFails()
      throws IOException, InterruptedException {
    when(githubService.createGithubIssue(any()))
        .thenThrow(
            new IOException("GitHub API returned HTTP 401: {\"message\":\"Bad credentials\"}"));
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertThat(failureReport.getIssueNumber(), is(nullValue()));
    assertThat(failureReport.getErrorDetail(), containsString("GitHub issue creation failed"));
    assertThat(failureReport.getErrorDetail(), containsString("HTTP 401"));
  }

  @Test
  void shouldRecordUserInfo() {
    User user = makeMe.aUser().please();
    request.setUserPrincipal(() -> user.getExternalIdentifier());
    currentUserFetcher = new CurrentUserFetcherFromRequest(request, userRepository, userService);
    controllerSetup =
        new ControllerSetup(failureReportRepository, currentUserFetcher, testabilitySettings);
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertThat(failureReport.getErrorDetail(), containsString(user.getExternalIdentifier()));
    assertThat(failureReport.getErrorDetail(), containsString(user.getName()));
  }

  @Test
  void shouldRecordRequestInfo() {
    request.setRequestURI("/path");
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertThat(failureReport.getErrorDetail(), containsString("/path"));
  }

  @Test
  void shouldNotRecordUserInfoWhenNoAuthentication() {
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertThat(failureReport.getErrorDetail(), containsString("user external Id: null"));
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
