package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Temporary legacy-content migration invoked by the post-Flyway startup listener. */
@Service
public class NumberedPropertyMigration {
  private final NumberedPropertyMigrationPreparation preparation;
  private final PropertyMemoryTrackerService trackers;
  private final AuthoredNoteDocumentPersistence documentPersistence;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final CanonicalDonutOrigin canonicalOrigin;
  private final NotebookRepository notebooks;

  public NumberedPropertyMigration(
      NumberedPropertyMigrationPreparation preparation,
      PropertyMemoryTrackerService trackers,
      AuthoredNoteDocumentPersistence documentPersistence,
      AcceptedWebChangeService acceptedWebChangeService,
      CanonicalDonutOrigin canonicalOrigin,
      NotebookRepository notebooks) {
    this.preparation = preparation;
    this.trackers = trackers;
    this.documentPersistence = documentPersistence;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.canonicalOrigin = canonicalOrigin;
    this.notebooks = notebooks;
  }

  /** Each accepted operation commits independently; retries inspect current authored content. */
  public Map<Integer, String> run(Timestamp updatedAt) throws UnexpectedNoAccessRightException {
    Map<Integer, String> diagnostics = new LinkedHashMap<>();
    for (Integer notebookId : notebooks.findAllIdsInOrder()) {
      String diagnostic = migrateNotebook(notebookId, updatedAt);
      if (diagnostic != null) diagnostics.put(notebookId, diagnostic);
    }
    return diagnostics;
  }

  /**
   * Returns a diagnostic with the complete operation unchanged when preflight cannot preserve it.
   */
  public String migrateNotebook(Integer notebookId, Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    var before = preparation.prepare(notebookId);
    if (before.diagnostic() != null) return before.diagnostic();
    return acceptedWebChangeService.apply(
        before.notebookIds(),
        () -> {
          var checked = preparation.prepare(notebookId);
          if (checked.diagnostic() != null) return checked.diagnostic();
          if (!before.notebookIds().equals(checked.notebookIds()))
            return "Affected notebooks changed; retry consolidation";
          for (var change : checked.changes()) {
            trackers.followConsolidatedProperties(change.note(), change.mapping().focuses());
            documentPersistence.persist(
                change.note(),
                AuthoredNoteDocument.fromContent(change.content(), canonicalOrigin),
                updatedAt);
          }
          return null;
        },
        ignored -> "Consolidate numbered properties",
        updatedAt);
  }
}
