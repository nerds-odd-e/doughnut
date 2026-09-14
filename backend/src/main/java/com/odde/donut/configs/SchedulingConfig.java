package com.odde.donut.configs;

import static com.odde.donut.DonutApplication.PORTABLE_TRASH_UPGRADE_PROFILE;

import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.services.GithubService;
import com.odde.donut.testability.TestabilitySettings;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("prod & !" + PORTABLE_TRASH_UPGRADE_PROFILE)
public class SchedulingConfig {

  @Bean
  public ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizer(
      GithubService githubService,
      FailureReportRepository failureReportRepository,
      TestabilitySettings testabilitySettings) {
    return scheduler ->
        scheduler.setErrorHandler(
            new ScheduledJobErrorHandler(
                githubService, failureReportRepository, testabilitySettings));
  }
}
