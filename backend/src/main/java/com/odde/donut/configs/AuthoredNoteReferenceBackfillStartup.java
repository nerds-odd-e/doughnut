package com.odde.donut.configs;

import com.odde.donut.services.AuthoredNoteReferenceBackfillTx;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * Runs the one-time {@code authored_note_reference} backfill for pre-existing notes at startup,
 * before the application becomes ready to serve indexed reads. Must run after {@link
 * FlyWayFreeVersionRealMigration}, whose {@code flyway.migrate()} creates the backfill's progress
 * table — see the {@code @Order} values on both listeners. A failed batch propagates out of this
 * {@link ApplicationReadyEvent} listener and fails application startup loudly, the same as a Flyway
 * migration failure; a restart resumes from the last committed batch, or is a no-op once complete.
 */
@Configuration
@Profile({"!test"})
public class AuthoredNoteReferenceBackfillStartup {

  private static final int BATCH_SIZE = 200;

  private final AuthoredNoteReferenceBackfillTx backfillTx;

  public AuthoredNoteReferenceBackfillStartup(AuthoredNoteReferenceBackfillTx backfillTx) {
    this.backfillTx = backfillTx;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(1)
  public void backfillAuthoredNoteReferences() {
    if (backfillTx.isComplete()) {
      return;
    }
    boolean hasMore = true;
    while (hasMore) {
      hasMore = backfillTx.processNextBatch(BATCH_SIZE);
    }
  }
}
