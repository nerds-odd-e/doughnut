package com.odde.doughnut.configs;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.factoryServices.FailureReportFactory;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ControllerSetup {
  @Autowired private final ModelFactoryService modelFactoryService;
  @Autowired private final CurrentUserFetcher currentUserFetcher;
  @Autowired private final TestabilitySettings testabilitySettings;

  public ControllerSetup(
      ModelFactoryService modelFactoryService,
      CurrentUserFetcher currentUserFetcher,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
    this.testabilitySettings = testabilitySettings;
  }

  @SneakyThrows
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleSystemException(HttpServletRequest req, Exception exception) {
    FailureReportFactory failureReportFactory =
        new FailureReportFactory(
            req,
            exception,
            currentUserFetcher,
            testabilitySettings.getGithubService(),
            modelFactoryService);
    failureReportFactory.createUnlessAllowed();

    throw exception;
  }

  @ExceptionHandler(OpenAiUnauthorizedException.class)
  public ResponseEntity<ApiError> handleOpenAIUnauthorizedException(
      OpenAiUnauthorizedException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getErrorBody());
  }

  @ExceptionHandler(com.odde.doughnut.exceptions.ApiException.class)
  public ResponseEntity<ApiError> handleApiException(
      com.odde.doughnut.exceptions.ApiException exception) {
    HttpStatus status = getHttpStatusForErrorType(exception.getErrorBody().getErrorType());
    return ResponseEntity.status(status).body(exception.getErrorBody());
  }

  private HttpStatus getHttpStatusForErrorType(ApiError.ErrorType errorType) {
    return switch (errorType) {
      case OPENAI_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
      case OPENAI_SERVICE_ERROR, WIKIDATA_SERVICE_ERROR, ASSESSMENT_SERVICE_ERROR ->
          HttpStatus.BAD_GATEWAY;
      case OPENAI_UNAUTHORIZED, QUESTION_ANSWER_ERROR, BINDING_ERROR -> HttpStatus.BAD_REQUEST;
    };
  }
}
