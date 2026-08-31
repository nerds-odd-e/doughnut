package com.odde.donut.factoryServices;

import com.odde.donut.controllers.currentUser.CurrentUserFetcher;
import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.GithubService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.web.server.ResponseStatusException;

public record FailureReportFactory(
    Exception exception,
    GithubService githubService,
    FailureReportRepository failureReportRepository,
    String contextPrefix,
    String origin) {

  public FailureReportFactory(
      HttpServletRequest req,
      Exception exception,
      CurrentUserFetcher currentUserFetcher,
      GithubService githubService,
      FailureReportRepository failureReportRepository) {
    this(
        exception,
        githubService,
        failureReportRepository,
        userInfo(currentUserFetcher) + requestInfo(req),
        httpOrigin(req));
  }

  public static FailureReportFactory fromException(
      Exception exception,
      String source,
      GithubService githubService,
      FailureReportRepository failureReportRepository) {
    return new FailureReportFactory(
        exception,
        githubService,
        failureReportRepository,
        "# source: " + source + "\n",
        "source:" + source);
  }

  public void createUnlessAllowed() {
    if (exception instanceof ResponseStatusException) return;
    if (exception instanceof ApiException) return;
    if (exception instanceof UnexpectedNoAccessRightException) return;

    FailureReport failureReport = createFailureReport();
    try {
      Integer issueNumber = githubService.createGithubIssue(failureReport);
      failureReport.setIssueNumber(issueNumber);
    } catch (Exception e) {
      failureReport.setErrorDetail(
          failureReport.getErrorDetail() + "\n# GitHub issue creation failed\n" + e.getMessage());
    }
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
    failureReport.setFingerprint(fingerprint());
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);
    failureReport.setErrorDetail(contextPrefix + "# Stack trace\n" + sw);
    saveFailureReport(failureReport);

    return failureReport;
  }

  private String fingerprint() {
    return exception.getClass().getName() + "|" + origin + "|" + applicationSite();
  }

  private String applicationSite() {
    StackTraceElement[] frames = exception.getStackTrace();
    for (StackTraceElement frame : frames) {
      if (frame.getClassName().startsWith("com.odde.donut")) {
        return classMethod(frame);
      }
    }
    return classMethod(frames[0]);
  }

  private static String classMethod(StackTraceElement frame) {
    String className = frame.getClassName();
    int lastDot = className.lastIndexOf('.');
    String simpleName = lastDot >= 0 ? className.substring(lastDot + 1) : className;
    return simpleName + "." + frame.getMethodName();
  }

  private static String httpOrigin(HttpServletRequest req) {
    return req.getMethod() + " " + req.getRequestURI().replaceAll("\\d+", "#");
  }

  private static String requestInfo(HttpServletRequest req) {
    return "# request:\n"
        + "  Request URI:"
        + req.getRequestURI()
        + "\n"
        + "  Request Query:"
        + req.getQueryString()
        + "\n";
  }

  private static String userInfo(CurrentUserFetcher currentUserFetcher) {
    String result = "# user external Id: " + currentUserFetcher.getExternalIdentifier() + "\n";
    if (currentUserFetcher.getUser() != null) {
      result += "# user name: " + currentUserFetcher.getUser().getName() + "\n";
    }
    return result;
  }
}
