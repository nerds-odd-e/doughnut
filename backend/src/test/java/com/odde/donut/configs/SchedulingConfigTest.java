package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.services.GithubService;
import com.odde.donut.testability.TestabilitySettings;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@ExtendWith(MockitoExtension.class)
class SchedulingConfigTest {

  @Mock GithubService githubService;
  @Mock FailureReportRepository failureReportRepository;

  @Test
  void recordsUncaughtFailureOfAVirtualThreadScheduledJobWithScheduledJobSource() {
    SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
    scheduler.setVirtualThreads(true);
    new SchedulingConfig()
        .simpleAsyncTaskSchedulerCustomizer(
            githubService, failureReportRepository, new TestabilitySettings())
        .customize(scheduler);
    scheduler.start();
    try {
      scheduler.schedule(
          () -> {
            throw new RuntimeException("scheduled boom");
          },
          Instant.now());

      ArgumentCaptor<FailureReport> reportCaptor = ArgumentCaptor.forClass(FailureReport.class);
      verify(failureReportRepository, timeout(5000).atLeastOnce()).save(reportCaptor.capture());
      assertThat(reportCaptor.getValue().getErrorDetail(), containsString("scheduled-job"));
    } finally {
      scheduler.close();
    }
  }
}
