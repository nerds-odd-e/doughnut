package com.odde.doughnut.configs;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlyWayFreeVersionIgnoreMigrationStrategyConfig {
  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> {
      // do nothing
    };
  }
}
