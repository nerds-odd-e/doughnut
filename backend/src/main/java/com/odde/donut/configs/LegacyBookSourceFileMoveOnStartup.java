package com.odde.donut.configs;

import com.odde.donut.services.book.LegacyBookSourceFileMove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * After Flyway's migration on startup, moves every Book still in the old Book storage into a file
 * at its notebook's root, one notebook per transaction. A notebook whose move fails is left
 * unchanged and logged; the others still move, and the next startup retries it.
 */
@Configuration
@Profile({"!test"})
public class LegacyBookSourceFileMoveOnStartup {
  private static final Logger logger =
      LoggerFactory.getLogger(LegacyBookSourceFileMoveOnStartup.class);
  private final LegacyBookSourceFileMove legacyBookSourceFileMove;

  public LegacyBookSourceFileMoveOnStartup(LegacyBookSourceFileMove legacyBookSourceFileMove) {
    this.legacyBookSourceFileMove = legacyBookSourceFileMove;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void moveLegacyBookSourceFiles() {
    legacyBookSourceFileMove
        .notebookIdsWithUnmovedBooks()
        .forEach(this::moveLeavingFailuresUnchanged);
  }

  private void moveLeavingFailuresUnchanged(Integer notebookId) {
    try {
      legacyBookSourceFileMove.move(notebookId);
    } catch (Exception e) {
      logger.error("Notebook {} keeps its Book in the old storage: the move failed", notebookId, e);
    }
  }
}
