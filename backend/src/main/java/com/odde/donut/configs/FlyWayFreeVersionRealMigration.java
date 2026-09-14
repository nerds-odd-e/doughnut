package com.odde.donut.configs;

import static com.odde.donut.DonutApplication.PORTABLE_TRASH_UPGRADE_PROFILE;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

@Configuration
@Profile("!test & !" + PORTABLE_TRASH_UPGRADE_PROFILE)
public class FlyWayFreeVersionRealMigration {
  @Autowired Flyway flyway;

  @EventListener(ApplicationReadyEvent.class)
  public void actualMigration() {
    flyway.repair();
    flyway.migrate();
  }
}
