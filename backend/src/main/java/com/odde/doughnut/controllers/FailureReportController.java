package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GithubService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/failure-reports")
class FailureReportController {
  private final ModelFactoryService modelFactoryService;
  private final GithubService realGithubService;
  private CurrentUser currentUser;
  private final AuthorizationService authorizationService;

  public FailureReportController(
      ModelFactoryService modelFactoryService,
      GithubService realGithubService,
      CurrentUser currentUser,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.realGithubService = realGithubService;
    this.currentUser = currentUser;
    this.authorizationService = authorizationService;
  }

  @GetMapping("")
  public Iterable<FailureReport> failureReports() throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn(currentUser.getUser());
    authorizationService.assertAdminAuthorization(currentUser.getUser());
    return modelFactoryService.failureReportRepository.findAll();
  }

  static class FailureReportForView {
    public FailureReport failureReport;
    public String githubIssueUrl;
  }

  @GetMapping("/{failureReport}")
  public FailureReportForView showFailureReport(
      @PathVariable("failureReport") @Schema(type = "integer") FailureReport failureReport)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn(currentUser.getUser());
    authorizationService.assertAdminAuthorization(currentUser.getUser());
    FailureReportForView failureReportForView = new FailureReportForView();
    failureReportForView.failureReport = failureReport;
    failureReportForView.githubIssueUrl =
        realGithubService.getIssueUrl(failureReport.getIssueNumber());
    return failureReportForView;
  }

  @DeleteMapping("/delete")
  public void deleteFailureReports(@RequestBody List<Integer> ids)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn(currentUser.getUser());
    authorizationService.assertAdminAuthorization(currentUser.getUser());
    ids.forEach(
        id -> {
          modelFactoryService
              .failureReportRepository
              .findById(id)
              .ifPresent(
                  failureReport ->
                      modelFactoryService.failureReportRepository.delete(failureReport));
        });
  }
}
