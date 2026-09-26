package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Rewrites notes stored with CRLF line endings to LF, each notebook's notes as one accepted web
 * change committed by the Donut System. Earlier history is left as it is.
 */
@Service
public class NoteLineEndingNormalization {
  private static final String CRLF_PATTERN = "%\r\n%";
  private final EntityPersister entityPersister;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final TestabilitySettings testabilitySettings;

  public NoteLineEndingNormalization(
      EntityPersister entityPersister,
      AcceptedWebChangeService acceptedWebChangeService,
      CanonicalDonutOrigin canonicalDonutOrigin,
      TestabilitySettings testabilitySettings) {
    this.entityPersister = entityPersister;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.testabilitySettings = testabilitySettings;
  }

  /** The notebooks holding a note whose content has a CRLF line ending. */
  public List<Integer> notebookIdsWithCrlfNotes() {
    return entityPersister
        .createQuery(
            "SELECT DISTINCT n.notebook.id FROM Note n WHERE n.content LIKE :crlf"
                + " ORDER BY n.notebook.id",
            Integer.class)
        .setParameter("crlf", CRLF_PATTERN)
        .getResultList();
  }

  public void normalize(Integer notebookId) throws UnexpectedNoAccessRightException {
    acceptedWebChangeService.apply(
        notebookId,
        () -> {
          for (Note note : crlfNotes(notebookId)) {
            note.replaceContent(
                AuthoredNoteDocument.fromContent(note.getContent(), canonicalDonutOrigin));
          }
          return null;
        },
        ignored -> "Normalize note line endings",
        testabilitySettings.getCurrentUTCTimestamp());
  }

  private List<Note> crlfNotes(Integer notebookId) {
    return entityPersister
        .createQuery(
            "FROM Note n WHERE n.notebook.id = :notebookId AND n.content LIKE :crlf", Note.class)
        .setParameter("notebookId", notebookId)
        .setParameter("crlf", CRLF_PATTERN)
        .getResultList();
  }
}
