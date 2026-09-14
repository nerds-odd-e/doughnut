package com.odde.donut;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.donut.configs.FlyWayFreeVersionRealMigration;
import com.odde.donut.configs.SchedulingConfig;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.services.GithubService;
import com.odde.donut.testability.TestabilitySettings;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

class DonutApplicationTest {

  @Test
  void portableTrashUpgradeStartsWithoutApplicationWriters() {
    var application =
        DonutApplication.applicationForTask(DonutApplication.PORTABLE_TRASH_UPGRADE_TASK);

    assertThat(application.getWebApplicationType(), is(WebApplicationType.NONE));
    assertThat(
        application.getAdditionalProfiles(),
        hasItem(DonutApplication.PORTABLE_TRASH_UPGRADE_PROFILE));

    try (var context = productionContext(DonutApplication.PORTABLE_TRASH_UPGRADE_PROFILE)) {
      assertThat(context.getBeansOfType(SchedulingConfig.class), anEmptyMap());
      assertThat(
          context.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME),
          is(false));
      assertThat(context.getBeansOfType(FlyWayFreeVersionRealMigration.class), anEmptyMap());
    }
  }

  @Test
  void ordinaryStartupKeepsWebSchedulingAndReadyMigrationConfiguration() {
    var application = DonutApplication.applicationForTask(null);

    assertThat(application.getWebApplicationType(), is(WebApplicationType.SERVLET));

    try (var context = productionContext()) {
      assertThat(context.getBeansOfType(SchedulingConfig.class), not(anEmptyMap()));
      assertThat(
          context.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME),
          is(true));
      assertThat(context.getBeansOfType(FlyWayFreeVersionRealMigration.class), not(anEmptyMap()));
    }
  }

  @Test
  void dispatchesPortableTrashUpgradeThroughTheTaskRunner() {
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    Flyway flyway = mock(Flyway.class);
    when(context.getBean(Flyway.class)).thenReturn(flyway);

    int exitCode =
        DonutApplication.runApplicationTask(context, DonutApplication.PORTABLE_TRASH_UPGRADE_TASK);

    assertThat(exitCode, is(0));
    verify(flyway).repair();
    verify(flyway).migrate();
    verify(context).close();
  }

  private AnnotationConfigApplicationContext productionContext(String... additionalProfiles) {
    var context = new AnnotationConfigApplicationContext();
    context.getEnvironment().setActiveProfiles("prod");
    for (String profile : additionalProfiles) {
      context.getEnvironment().addActiveProfile(profile);
    }
    context.registerBean(Flyway.class, () -> mock(Flyway.class));
    context.registerBean(GithubService.class, () -> mock(GithubService.class));
    context.registerBean(FailureReportRepository.class, () -> mock(FailureReportRepository.class));
    context.registerBean(TestabilitySettings.class, TestabilitySettings::new);
    context.register(SchedulingConfig.class, FlyWayFreeVersionRealMigration.class);
    context.refresh();
    return context;
  }
}
