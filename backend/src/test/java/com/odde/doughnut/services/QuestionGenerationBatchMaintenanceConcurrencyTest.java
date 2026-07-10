package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.configs.ShedLockConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:mysql://127.0.0.1:3309/doughnut_test",
      "spring.datasource.username=doughnut",
      "spring.datasource.password=doughnut",
      "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
    })
@Import(DataSourceAutoConfiguration.class)
@ActiveProfiles("test")
class QuestionGenerationBatchMaintenanceConcurrencyTest {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void jdbcLockProviderPreventsDuplicateMaintenanceExecution() {
    String lockName = "qgb-hourly-lock-test-" + (Instant.now().toEpochMilli() % 1_000_000_000L);
    LockProvider lockProvider =
        new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbcTemplate)
                .usingDbTime()
                .build());
    LockConfiguration lockConfiguration =
        new LockConfiguration(
            Instant.now(), lockName, Duration.ofMinutes(55), Duration.ofMinutes(1));

    Optional<SimpleLock> firstLock = lockProvider.lock(lockConfiguration);
    try {
      Optional<SimpleLock> secondLock = lockProvider.lock(lockConfiguration);

      assertThat(firstLock.isPresent(), is(true));
      assertThat(secondLock.isPresent(), is(false));
    } finally {
      firstLock.ifPresent(SimpleLock::unlock);
      jdbcTemplate.update("DELETE FROM shedlock WHERE name = ?", lockName);
    }
  }
}

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {ShedLockConfig.class},
    properties = {
      "spring.datasource.url=jdbc:mysql://127.0.0.1:3309/doughnut_test",
      "spring.datasource.username=doughnut",
      "spring.datasource.password=doughnut",
      "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
    })
@Import(DataSourceAutoConfiguration.class)
@ActiveProfiles("prod")
class ShedLockConfigProdTest {

  @Autowired LockProvider lockProvider;

  @Test
  void loadsJdbcTemplateLockProviderUnderProdProfile() {
    assertThat(lockProvider, notNullValue());
    assertThat(lockProvider, instanceOf(JdbcTemplateLockProvider.class));
  }

  @Test
  void shedLockConfigIsProdOnlyAndEnablesSchedulerLock() {
    assertThat(ShedLockConfig.class.isAnnotationPresent(Profile.class), is(true));
    assertThat(ShedLockConfig.class.getAnnotation(Profile.class).value()[0], equalTo("prod"));
    assertThat(ShedLockConfig.class.isAnnotationPresent(EnableSchedulerLock.class), is(true));
  }
}
