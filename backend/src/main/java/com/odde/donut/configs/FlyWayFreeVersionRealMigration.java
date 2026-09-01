package com.odde.donut.configs;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

@Configuration
@Profile({"!test"})
public class FlyWayFreeVersionRealMigration {
  @Autowired Flyway flyway;

  // Runs before AuthoredNoteReferenceBackfillStartup (@Order(1)), whose backfill needs the schema
  // this migration creates.
  @EventListener(ApplicationReadyEvent.class)
  @Order(0)
  public void actualMigration() {
    flyway.repair();
    flyway.migrate();
  }
}
