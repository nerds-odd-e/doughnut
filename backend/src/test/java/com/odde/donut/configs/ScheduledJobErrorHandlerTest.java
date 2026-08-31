package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.services.GithubService;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(MockitoExtension.class)
class ScheduledJobErrorHandlerTest {

  @Autowired FailureReportRepository failureReportRepository;
  @Mock GithubService githubService;

  ScheduledJobErrorHandler handler;

  @BeforeEach
  void setUp() throws IOException, InterruptedException {
    doReturn(null).when(githubService).createGithubIssue(any());
    handler = new ScheduledJobErrorHandler(githubService, failureReportRepository);
  }

  @Test
  void handleErrorPersistsOneFailureReportWithScheduledJobSource() {
    handler.handleError(new RuntimeException("scheduled boom"));

    Iterable<FailureReport> reports = failureReportRepository.findAll();
    assertThat(reports, iterableWithSize(1));
    assertThat(reports.iterator().next().getErrorDetail(), containsString("scheduled-job"));
  }
}
