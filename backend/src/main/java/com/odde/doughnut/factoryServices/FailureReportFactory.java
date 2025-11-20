package com.odde.doughnut.factoryServices;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.GithubService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.web.server.ResponseStatusException;

public record FailureReportFactory(
    HttpServletRequest req,
    Exception exception,
    CurrentUserFetcher currentUserFetcher,
    GithubService githubService,
    FailureReportRepository failureReportRepository) {

  public void createUnlessAllowed() throws IOException, InterruptedException {
    if (exception instanceof ResponseStatusException) return;
    if (exception instanceof UnexpectedNoAccessRightException) return;

    FailureReport failureReport = createFailureReport();
    Integer issueNumber = githubService.createGithubIssue(failureReport);
    failureReport.setIssueNumber(issueNumber);
    saveFailureReport(failureReport);
  }

  private FailureReport saveFailureReport(FailureReport failureReport) {
    // it has to use repository directly because
    // a transaction may not be available when handling exception
    return failureReportRepository.save(failureReport);
  }

  private FailureReport createFailureReport() {
    FailureReport failureReport = new FailureReport();
    failureReport.setErrorName(exception.getClass().getName());
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);
    failureReport.setErrorDetail(getUserInfo() + getRequestInfo() + "# Stack trace\n" + sw);
    saveFailureReport(failureReport);

    return failureReport;
  }

  private String getRequestInfo() {
    return "# request:\n"
        + "  Request URI:"
        + req.getRequestURI()
        + "\n"
        + "  Request Query:"
        + req.getQueryString()
        + "\n";
  }

  private String getUserInfo() {
    String result = "# user external Id: " + currentUserFetcher.getExternalIdentifier() + "\n";
    if (currentUserFetcher.getUser() != null) {
      result += "# user name: " + currentUserFetcher.getUser().getName() + "\n";
    }
    return result;
  }
}
