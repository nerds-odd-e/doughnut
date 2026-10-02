package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.DonutApplication;
import java.time.Duration;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Shared post-Flyway observations; no additional Boot context or pool. */
abstract class NumberedPropertyStartupTestSupport
    extends NumberedPropertyMigrationCommittedTestSupport {
  void publishReady(AnnotationConfigApplicationContext startup) {
    startup.publishEvent(
        new ApplicationReadyEvent(
            new SpringApplication(DonutApplication.class), new String[0], startup, Duration.ZERO));
  }

  static class ReadyObservation {
    private final JdbcTemplate database;
    private final Flyway flyway;
    private final List<String> observed;
    private final Runnable migratedContent;

    ReadyObservation(
        JdbcTemplate database, Flyway flyway, List<String> observed, Runnable migratedContent) {
      this.database = database;
      this.flyway = flyway;
      this.observed = observed;
      this.migratedContent = migratedContent;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public void consume() {
      assertThat(flyway.info().pending().length, is(0));
      assertThat(TransactionSynchronizationManager.isActualTransactionActive(), is(false));
      migratedContent.run();
      observed.add("migrated consumer");
      assertThat(
          database.queryForList(
              "SELECT CONCAT(column_name, ':', character_maximum_length, ':', collation_name) "
                  + "FROM information_schema.columns WHERE table_schema = DATABASE() "
                  + "AND table_name = 'memory_tracker' "
                  + "AND column_name IN ('property_key', 'property_value') ORDER BY column_name",
              String.class),
          is(
              List.of(
                  "property_key:255:utf8mb4_0900_ai_ci", "property_value:255:utf8mb4_0900_ai_ci")));
      assertThat(
          database.queryForList(
              "SELECT column_name FROM information_schema.statistics "
                  + "WHERE table_schema = DATABASE() AND table_name = 'memory_tracker' "
                  + "AND index_name = 'uq_memory_tracker_user_note_type_property' "
                  + "AND non_unique = 0 ORDER BY seq_in_index",
              String.class),
          is(List.of("user_id", "note_id", "type", "property_key", "property_value")));
    }
  }
}
