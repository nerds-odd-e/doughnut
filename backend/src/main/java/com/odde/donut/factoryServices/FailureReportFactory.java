package com.odde.donut.factoryServices;

import com.odde.donut.controllers.currentUser.CurrentUserFetcher;
import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.GithubService;
import com.odde.donut.utils.TimestampOperations;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;

public record FailureReportFactory(
    Exception exception,
    GithubService githubService,
    FailureReportRepository failureReportRepository,
    String contextPrefix,
    String origin,
    Timestamp currentUTCTimestamp) {

  static final int GITHUB_COUNT_COMMENT_DEBOUNCE_HOURS = 6;

  public FailureReportFactory(
      HttpServletRequest req,
      Exception exception,
      CurrentUserFetcher currentUserFetcher,
      GithubService githubService,
      FailureReportRepository failureReportRepository,
      Timestamp currentUTCTimestamp) {
    this(
        exception,
        githubService,
        failureReportRepository,
        userInfo(currentUserFetcher) + requestInfo(req),
        httpOrigin(req),
        currentUTCTimestamp);
  }

  public static FailureReportFactory fromException(
      Exception exception,
      String source,
      GithubService githubService,
      FailureReportRepository failureReportRepository,
      Timestamp currentUTCTimestamp) {
    return new FailureReportFactory(
        exception,
        githubService,
        failureReportRepository,
        "# source: " + source + "\n",
        "source:" + source,
        currentUTCTimestamp);
  }

  public void createUnlessAllowed() {
    if (exception instanceof ResponseStatusException) return;
    if (exception instanceof ApiException) return;
    if (exception instanceof UnexpectedNoAccessRightException) return;

    if (incrementIfConsecutiveSimilar()) return;

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

  private boolean incrementIfConsecutiveSimilar() {
    Optional<FailureReport> latest = failureReportRepository.findTopByOrderByIdDesc();
    if (latest.isEmpty() || !fingerprint().equals(latest.get().getFingerprint())) {
      return false;
    }
    FailureReport report = latest.get();
    report.setOccurrenceCount(report.getOccurrenceCount() + 1);
    saveFailureReport(report);
    commentGithubIssueIfDue(report);
    return true;
  }

  private void commentGithubIssueIfDue(FailureReport report) {
    if (report.getIssueNumber() == null) {
      return;
    }
    if (!githubCountCommentIsDue(report)) {
      return;
    }
    try {
      githubService.commentOnGithubIssue(
          report.getIssueNumber(), String.valueOf(report.getOccurrenceCount()));
      report.setLastGithubCommentDatetime(currentUTCTimestamp);
      saveFailureReport(report);
    } catch (Exception ignored) {
      // GitHub comment is best-effort; the Failure report already has the count
    }
  }

  private boolean githubCountCommentIsDue(FailureReport report) {
    Timestamp lastComment = report.getLastGithubCommentDatetime();
    if (lastComment == null) {
      return true;
    }
    return !currentUTCTimestamp.before(
        TimestampOperations.addHoursToTimestamp(lastComment, GITHUB_COUNT_COMMENT_DEBOUNCE_HOURS));
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
