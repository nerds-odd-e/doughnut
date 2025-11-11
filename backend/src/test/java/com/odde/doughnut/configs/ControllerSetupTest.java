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
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.RealGithubService;
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
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ControllerSetupTest {
  @Autowired MakeMe makeMe;
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired UserRepository userRepository;
  @Mock RealGithubService githubService;
  MockHttpServletRequest request = new MockHttpServletRequest();
  @Mock TestabilitySettings testabilitySettings;

  ControllerSetup controllerSetup;
  CurrentUserFetcherFromRequest currentUserFetcher;

  @BeforeEach
  void setup() {
    when(testabilitySettings.getGithubService()).thenReturn(githubService);
    currentUserFetcher =
        new CurrentUserFetcherFromRequest(request, userRepository, modelFactoryService);
    controllerSetup =
        new ControllerSetup(this.modelFactoryService, currentUserFetcher, testabilitySettings);
  }

  @Test
  void shouldNotRecordResponseStatusException() {
    long count = makeMe.modelFactoryService.failureReportRepository.count();
    assertThrows(
        ResponseStatusException.class,
        () ->
            controllerSetup.handleSystemException(
                request, new ResponseStatusException(HttpStatus.UNAUTHORIZED, "xx")));
    assertThat(makeMe.modelFactoryService.failureReportRepository.count(), equalTo(count));
  }

  @Test
  void shouldNotRecordUnexpectedNoAccessRightException() {
    long count = makeMe.modelFactoryService.failureReportRepository.count();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            controllerSetup.handleSystemException(request, new UnexpectedNoAccessRightException()));
    assertThat(makeMe.modelFactoryService.failureReportRepository.count(), equalTo(count));
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
  void shouldRecordUserInfo() {
    User user = makeMe.aUser().please();
    request.setUserPrincipal(() -> user.getExternalIdentifier());
    currentUserFetcher =
        new CurrentUserFetcherFromRequest(request, userRepository, modelFactoryService);
    controllerSetup =
        new ControllerSetup(this.modelFactoryService, currentUserFetcher, testabilitySettings);
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

  private FailureReport catchExceptionAndGetFailureReport() {
    assertThrows(
        RuntimeException.class,
        () -> controllerSetup.handleSystemException(request, new RuntimeException()));
    return makeMe.modelFactoryService.failureReportRepository.findAll().iterator().next();
  }
}
