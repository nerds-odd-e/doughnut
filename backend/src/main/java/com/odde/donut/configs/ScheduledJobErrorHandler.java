package com.odde.donut.configs;

import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.factoryServices.FailureReportFactory;
import com.odde.donut.services.GithubService;
import com.odde.donut.testability.TestabilitySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ErrorHandler;

public class ScheduledJobErrorHandler implements ErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(ScheduledJobErrorHandler.class);
  private static final String SOURCE = "scheduled-job";

  private final GithubService githubService;
  private final FailureReportRepository failureReportRepository;
  private final TestabilitySettings testabilitySettings;

  public ScheduledJobErrorHandler(
      GithubService githubService,
      FailureReportRepository failureReportRepository,
      TestabilitySettings testabilitySettings) {
    this.githubService = githubService;
    this.failureReportRepository = failureReportRepository;
    this.testabilitySettings = testabilitySettings;
  }

  @Override
  public void handleError(Throwable t) {
    logger.error("Unexpected error occurred in scheduled task", t);
    if (t instanceof Exception exception) {
      FailureReportFactory.fromException(
              exception,
              SOURCE,
              githubService,
              failureReportRepository,
              testabilitySettings.getCurrentUTCTimestamp())
          .createUnlessAllowed();
    }
  }
}
