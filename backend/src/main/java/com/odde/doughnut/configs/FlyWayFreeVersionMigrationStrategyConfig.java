package com.odde.doughnut.configs;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class FlyWayFreeVersionMigrationStrategyConfig {
    @Autowired
    Flyway flyway;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // do nothing
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    public void actualMigration() {
        flyway.repair();
        flyway.migrate();
    }
}