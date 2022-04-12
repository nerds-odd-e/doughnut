package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;

public class FailureReportTest {
  MakeMe makeMe = new MakeMe();

  @Test
  void githubIssue() {
    FailureReport failureReport = new FailureReport();
    failureReport.setErrorName("xxx");
    failureReport.setErrorDetail("err detail");
    final FailureReport.GithubIssue githubIssue = failureReport.getGithubIssue();
    assertThat(githubIssue.getBody(), not(containsString("err detail")));
    assertThat(
        githubIssue.getBody(),
        containsString("https://doughnut.odd-e.com/failure-report-list/show/null"));
  }
}
