package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.FailureReportService;
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
  private final FailureReportService failureReportService;
  private final GithubService realGithubService;
  private final AuthorizationService authorizationService;

  public FailureReportController(
      FailureReportService failureReportService,
      GithubService realGithubService,
      AuthorizationService authorizationService) {
    this.failureReportService = failureReportService;
    this.realGithubService = realGithubService;
    this.authorizationService = authorizationService;
  }

  @GetMapping("")
  public Iterable<FailureReport> failureReports() throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return failureReportService.getAllFailureReports();
  }

  static class FailureReportForView {
    public FailureReport failureReport;
    public String githubIssueUrl;
  }

  @GetMapping("/{failureReport}")
  public FailureReportForView showFailureReport(
      @PathVariable("failureReport") @Schema(type = "integer") FailureReport failureReport)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    FailureReportForView failureReportForView = new FailureReportForView();
    failureReportForView.failureReport = failureReport;
    failureReportForView.githubIssueUrl =
        realGithubService.getIssueUrl(failureReport.getIssueNumber());
    return failureReportForView;
  }

  @DeleteMapping("/delete")
  public void deleteFailureReports(@RequestBody List<Integer> ids)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    failureReportService.deleteFailureReports(ids);
  }
}
