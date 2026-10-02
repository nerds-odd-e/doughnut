package com.odde.donut.configs;

import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NumberedPropertyMigration;
import java.sql.Timestamp;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Temporary content migration, synchronous and ordered after Flyway repair/migrate. */
@Configuration
@Profile({"!test"})
public class NumberedPropertyStartupMigration {
  private static final Logger logger =
      LoggerFactory.getLogger(NumberedPropertyStartupMigration.class);
  private final NumberedPropertyMigration migration;

  public NumberedPropertyStartupMigration(NumberedPropertyMigration migration) {
    this.migration = migration;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(Ordered.HIGHEST_PRECEDENCE + 1)
  public void consolidate() throws UnexpectedNoAccessRightException {
    migration
        .run(Timestamp.from(Instant.now()))
        .forEach(
            (notebookId, diagnostic) ->
                logger.warn(
                    "Numbered-property migration left notebook {} unchanged: {}",
                    notebookId,
                    diagnostic));
  }
}
