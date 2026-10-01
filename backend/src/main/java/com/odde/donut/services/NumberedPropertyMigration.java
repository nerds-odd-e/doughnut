package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NoteContentMarkdown.ConsolidatedProperties;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Temporary legacy-content migration. The startup caller is installed after its full journey is
 * proved.
 */
@Service
public class NumberedPropertyMigration {
  private final NoteRepository noteRepository;
  private final PropertyMemoryTrackerService propertyMemoryTrackerService;
  private final AuthoredNoteDocumentPersistence documentPersistence;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final CanonicalDonutOrigin canonicalOrigin;

  public NumberedPropertyMigration(
      NoteRepository noteRepository,
      PropertyMemoryTrackerService propertyMemoryTrackerService,
      AuthoredNoteDocumentPersistence documentPersistence,
      AcceptedWebChangeService acceptedWebChangeService,
      CanonicalDonutOrigin canonicalOrigin) {
    this.noteRepository = noteRepository;
    this.propertyMemoryTrackerService = propertyMemoryTrackerService;
    this.documentPersistence = documentPersistence;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.canonicalOrigin = canonicalOrigin;
  }

  /** Returns a diagnostic without mutation when any source or persisted focus cannot be mapped. */
  public String migrateNotebook(Integer notebookId, Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    return acceptedWebChangeService.apply(
        notebookId,
        () -> {
          List<PreparedNote> prepared = new ArrayList<>();
          for (Note note : noteRepository.findAllByNotebookIdOrderByIdAsc(notebookId)) {
            ConsolidatedProperties transformed =
                NoteContentMarkdown.consolidateNumberedProperties(note.getContent());
            String diagnostic = transformed.diagnostic();
            if (diagnostic == null) {
              diagnostic =
                  propertyMemoryTrackerService.consolidationDiagnostic(
                      note, transformed.focuses(), transformed.sourceKeys());
            }
            if (diagnostic != null) {
              return "Note " + note.getId() + ": " + diagnostic;
            }
            if (!transformed.sourceKeys().isEmpty()) {
              prepared.add(new PreparedNote(note, transformed));
            }
          }
          for (PreparedNote change : prepared) {
            propertyMemoryTrackerService.followConsolidatedProperties(
                change.note(), change.transformed().focuses());
            documentPersistence.persist(
                change.note(),
                AuthoredNoteDocument.fromContent(change.transformed().content(), canonicalOrigin),
                updatedAt);
          }
          return null;
        },
        ignored -> "Consolidate numbered properties",
        updatedAt);
  }

  private record PreparedNote(Note note, ConsolidatedProperties transformed) {}
}
