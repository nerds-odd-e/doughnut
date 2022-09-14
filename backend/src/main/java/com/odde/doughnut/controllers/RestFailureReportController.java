package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.RealGithubService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/failure-reports")
class RestFailureReportController {
  private final ModelFactoryService modelFactoryService;
  private final RealGithubService realGithubService;
  private UserModel currentUser;

  public RestFailureReportController(
      ModelFactoryService modelFactoryService,
      RealGithubService realGithubService,
      UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.realGithubService = realGithubService;
    this.currentUser = currentUser;
  }

  @GetMapping("")
  public Iterable<FailureReport> failureReports() throws NoAccessRightException {
    currentUser.assertDeveloperAuthorization();
    return modelFactoryService.failureReportRepository.findAll();
  }

  static class FailureReportForView {
    public FailureReport failureReport;
    public String githubIssueUrl;
  }

  @GetMapping("/{failureReport}")
  public FailureReportForView failureReport(FailureReport failureReport)
      throws NoAccessRightException {
    currentUser.assertDeveloperAuthorization();
    FailureReportForView failureReportForView = new FailureReportForView();
    failureReportForView.failureReport = failureReport;
    failureReportForView.githubIssueUrl =
        realGithubService.getIssueUrl(failureReport.getIssueNumber());
    return failureReportForView;
  }
}
