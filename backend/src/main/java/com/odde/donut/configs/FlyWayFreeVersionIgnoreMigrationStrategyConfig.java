package com.odde.donut.configs;

import static com.odde.donut.DonutApplication.PORTABLE_TRASH_UPGRADE_PROFILE;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"!test", PORTABLE_TRASH_UPGRADE_PROFILE})
public class FlyWayFreeVersionIgnoreMigrationStrategyConfig {
  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> {
      // do nothing
    };
  }
}
