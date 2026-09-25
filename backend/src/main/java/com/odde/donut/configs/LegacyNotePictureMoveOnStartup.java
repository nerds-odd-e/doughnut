package com.odde.donut.configs;

import com.odde.donut.services.notebookGit.LegacyNotePictureMove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * After Flyway's migration on startup, moves every notebook's own legacy uploaded pictures into
 * files beside their notes, one notebook per transaction. A notebook whose move fails is left
 * unchanged and logged; the others still move, and the next startup retries it.
 */
@Configuration
@Profile({"!test"})
public class LegacyNotePictureMoveOnStartup {
  private static final Logger logger =
      LoggerFactory.getLogger(LegacyNotePictureMoveOnStartup.class);
  private final LegacyNotePictureMove legacyNotePictureMove;

  public LegacyNotePictureMoveOnStartup(LegacyNotePictureMove legacyNotePictureMove) {
    this.legacyNotePictureMove = legacyNotePictureMove;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void moveLegacyPictures() {
    legacyNotePictureMove
        .notebookIdsWithLegacyReferences()
        .forEach(this::moveLeavingFailuresUnchanged);
  }

  private void moveLeavingFailuresUnchanged(Integer notebookId) {
    try {
      int skippedReferences = legacyNotePictureMove.move(notebookId);
      if (skippedReferences > 0) {
        logger.warn(
            "Notebook {} keeps {} legacy picture references to uploads it does not own",
            notebookId,
            skippedReferences);
      }
    } catch (Exception e) {
      logger.error("Notebook {} keeps its legacy pictures: the move failed", notebookId, e);
    }
  }
}
