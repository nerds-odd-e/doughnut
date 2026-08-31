package com.odde.donut.configs;

import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.services.GithubService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@Profile("prod")
public class SchedulingConfig {

  @Bean
  public ThreadPoolTaskScheduler taskScheduler(
      GithubService githubService, FailureReportRepository failureReportRepository) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setErrorHandler(new ScheduledJobErrorHandler(githubService, failureReportRepository));
    return scheduler;
  }
}
