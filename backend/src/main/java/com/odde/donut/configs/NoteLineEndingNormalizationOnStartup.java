package com.odde.donut.configs;

import com.odde.donut.services.notebookGit.NoteLineEndingNormalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * After Flyway's migration on startup, normalizes every notebook's CRLF notes to LF, one notebook
 * at a time. A notebook whose normalization fails is left unchanged and logged; the others still
 * proceed, and the next startup retries it.
 */
@Configuration
@Profile({"!test"})
public class NoteLineEndingNormalizationOnStartup {
  private static final Logger logger =
      LoggerFactory.getLogger(NoteLineEndingNormalizationOnStartup.class);
  private final NoteLineEndingNormalization noteLineEndingNormalization;

  public NoteLineEndingNormalizationOnStartup(
      NoteLineEndingNormalization noteLineEndingNormalization) {
    this.noteLineEndingNormalization = noteLineEndingNormalization;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void normalizeLineEndings() {
    noteLineEndingNormalization
        .notebookIdsWithCrlfNotes()
        .forEach(this::normalizeLeavingFailuresUnchanged);
  }

  private void normalizeLeavingFailuresUnchanged(Integer notebookId) {
    try {
      noteLineEndingNormalization.normalize(notebookId);
    } catch (Exception e) {
      logger.error(
          "Notebook {} keeps its CRLF notes: the line ending normalization failed", notebookId, e);
    }
  }
}
