package com.odde.donut.configs;

import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.services.GithubService;
import com.odde.donut.testability.TestabilitySettings;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("prod")
public class SchedulingConfig {

  @Bean
  public SimpleAsyncTaskSchedulerCustomizer simpleAsyncTaskSchedulerCustomizer(
      GithubService githubService,
      FailureReportRepository failureReportRepository,
      TestabilitySettings testabilitySettings) {
    return scheduler ->
        scheduler.setErrorHandler(
            new ScheduledJobErrorHandler(
                githubService, failureReportRepository, testabilitySettings));
  }
}
