package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.controllers.json.ApiError;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.RealGithubService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class ControllerSetupTest {
  @Autowired MakeMe makeMe;
  @Autowired ModelFactoryService modelFactoryService;
  @Mock RealGithubService githubService;
  @Mock CurrentUserFetcher currentUserFetcher;
  MockHttpServletRequest request = new MockHttpServletRequest();
  @Mock TestabilitySettings testabilitySettings;

  ControllerSetup controllerSetup;

  @BeforeEach
  void setup() {
    when(testabilitySettings.getGithubService()).thenReturn(githubService);
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
    UserModel userModel = makeMe.aUser().toModelPlease();
    String externalId = userModel.getEntity().getExternalIdentifier();
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
    when(currentUserFetcher.getUser()).thenReturn(userModel);
    FailureReport failureReport = catchExceptionAndGetFailureReport();
    assertThat(failureReport.getErrorDetail(), containsString(externalId));
    assertThat(failureReport.getErrorDetail(), containsString(userModel.getName()));
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
    assertThat(response.getBody().getErrors().keySet(), contains("_originalMessage"));
    assertThat(response.getBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_UNAUTHORIZED));
  }

  private FailureReport catchExceptionAndGetFailureReport() {
    assertThrows(
        RuntimeException.class,
        () -> controllerSetup.handleSystemException(request, new RuntimeException()));
    return makeMe.modelFactoryService.failureReportRepository.findAll().iterator().next();
  }
}
