package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FailureReportTest {
    MakeMe makeMe = new MakeMe();

    @Test
    void githubIssue() {
        FailureReport failureReport = new FailureReport();
        failureReport.setErrorName("xxx");
        failureReport.setErrorDetail("err detail");
        final FailureReport.GithubIssue githubIssue = failureReport.getGithubIssue();
        assertThat(githubIssue.getBody(), not(containsString("err detail")));
        assertThat(githubIssue.getBody(), containsString("https://doughnut.odd-e.com/failure-report-list/show/null"));
    }
}

