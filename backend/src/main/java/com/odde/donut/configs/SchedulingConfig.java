package com.odde.donut.configs;

import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.services.GithubService;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("prod")
public class SchedulingConfig {

  @Bean
  public ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizer(
      GithubService githubService, FailureReportRepository failureReportRepository) {
    return scheduler ->
        scheduler.setErrorHandler(
            new ScheduledJobErrorHandler(githubService, failureReportRepository));
  }
}
